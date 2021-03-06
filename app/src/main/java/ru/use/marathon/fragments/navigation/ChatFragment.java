package ru.use.marathon.fragments.navigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.use.marathon.AppController;
import ru.use.marathon.Constants;
import ru.use.marathon.R;
import ru.use.marathon.activities.AllUsersActivity;
import ru.use.marathon.activities.ChatRoomActivity;
import ru.use.marathon.adapters.chat.ChatRoomsAdapter;
import ru.use.marathon.fragments.AbstractFragment;
import ru.use.marathon.models.Success;
import ru.use.marathon.models.chat.ChatRoom;
import ru.use.marathon.models.chat.Rooms;
import ru.use.marathon.utils.ItemClickSupport;
import ru.use.marathon.utils.NotificationUtils;

import static ru.use.marathon.models.Success.success;

/**
 * Created by ilyas on 06-Jul-18.
 */

public class ChatFragment extends AbstractFragment {

    public static final String TAG = ChatFragment.class.getSimpleName();
    private BroadcastReceiver mRegistrationBroadcastReceiver;


    ArrayList<ChatRoom> roomArrayList;
    ChatRoomsAdapter adapter;

    @BindView(R.id.chat_rooms_rv)
    RecyclerView recyclerView;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.rv_status)
    TextView status;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        ButterKnife.bind(this, view);

        updateRegToken();

        roomArrayList = new ArrayList<>();
        initRecyclerView();

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.PUSH_NOTIFICATION)) {
                    int chat_id = Integer.parseInt(intent.getStringExtra("chat_id"));
                    String name = intent.getStringExtra("name");
                    String message = intent.getStringExtra("message");
                    String timestamp = intent.getStringExtra("timestamp");

                    for (ChatRoom rc : roomArrayList) {
                        if (rc.getId() == chat_id) {
                            rc.setLastMessage(message);
                            rc.setUnreadCount(rc.getUnreadCount() + 1);
                            rc.setName(name);
                            rc.setTimestamp(timestamp);
                        }
                    }
                    adapter.notifyDataSetChanged();

                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "", Toast.LENGTH_SHORT).show();
                }
            }
        };

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity().getApplicationContext(), AllUsersActivity.class);
                if (userType() == STUDENT) {
                    i.putExtra("type", 1);
                } else if (userType() == TEACHER) {
                    i.putExtra("type", 0);
                }
                startActivity(i);
            }
        });

        return view;
    }

    private void initRecyclerView() {
        status.setVisibility(View.INVISIBLE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getActivity().getApplicationContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
        int resId = R.anim.layout_animation_fall_down;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getActivity().getApplicationContext(), resId);
        recyclerView.setLayoutAnimation(animation);
        adapter = new ChatRoomsAdapter(getActivity().getApplicationContext(), roomArrayList);
        recyclerView.setAdapter(adapter);

        showLoadDialog(getContext(), "Пожалуйста подожите", "Грузим сообщения..");
        getChatRooms();


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    fab.show();
                }

                super.onScrollStateChanged(recyclerView, newState);

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 || dy < 0 && fab.isShown()) {
                    fab.hide();
                }
            }
        });
        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {

                int id = roomArrayList.get(position).getId();
                String title = roomArrayList.get(position).getTitle();
                String name = roomArrayList.get(position).getName();
                String time = roomArrayList.get(position).getTimestamp();
                String message = roomArrayList.get(position).getLastMessage();

                roomArrayList.remove(position);
                roomArrayList.add(position, new ChatRoom(id, 0, title, name, time, "", message));

                recyclerView.getAdapter().notifyDataSetChanged();

                Intent i = new Intent(getActivity(), ChatRoomActivity.class);
                i.putExtra("title", title);
                i.putExtra("chat_id", id);
                startActivityForResult(i, 100);


            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            roomArrayList.clear();
            getChatRooms();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Constants.PUSH_NOTIFICATION));
        NotificationUtils.clearNotifications(getActivity().getApplicationContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mRegistrationBroadcastReceiver);
    }

    private void getChatRooms() {
        AppController.getApi().getChatRooms(1, "getChatRooms", user_id(), userType()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                new Success(response);
                if (!success()) {
                    closeLoadDialog();
                    status.setText("Нажмите на + чтобы создать первый чат!");
                    status.setVisibility(View.VISIBLE);
                } else {
                    Rooms rooms = new Rooms(response);
                    for (int i = 0; i < rooms.size(); i++) {
                        ChatRoom cr = new ChatRoom(rooms.getChatId(i), 0, rooms.getTitle(i), rooms.getName(i), rooms.getTimestamp(i), "", rooms.getLastMessage(i));
                        roomArrayList.add(cr);
                    }
                    if (roomArrayList.size() > 0) {
                        status.setVisibility(View.GONE);
                    } else {
                        status.setText("Нажмите на + чтобы создать первый чат!");
                        status.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                    closeLoadDialog();
                    runLayoutAnimation(recyclerView);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }



}
