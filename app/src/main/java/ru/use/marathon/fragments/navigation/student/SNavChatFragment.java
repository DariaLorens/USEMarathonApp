package ru.use.marathon.fragments.navigation.student;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.use.marathon.AppController;
import ru.use.marathon.Constants;
import ru.use.marathon.R;
import ru.use.marathon.activities.ChatRoomActivity;
import ru.use.marathon.adapters.chat.ChatRoomsAdapter;
import ru.use.marathon.models.Student;
import ru.use.marathon.models.Success;
import ru.use.marathon.models.chat.ChatRoom;
import ru.use.marathon.models.chat.Rooms;
import ru.use.marathon.utils.ItemClickSupport;
import ru.use.marathon.utils.NotificationUtils;

import static ru.use.marathon.models.Success.success;

/**
 * Created by ilyas on 06-Jul-18.
 */

public class SNavChatFragment extends Fragment{

    public static final String TAG = SNavChatFragment.class.getSimpleName();
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    Student student;
    int user_id,utype = 0;
    String user_name;

    ArrayList<ChatRoom> roomArrayList;
    ChatRoomsAdapter adapter;

    @BindView(R.id.chat_rooms_rv)
    RecyclerView recyclerView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_chat,container,false);
        ButterKnife.bind(this,view);

        student = new Student(getActivity().getApplicationContext());

        HashMap<String,String> stu_data = student.getData();
        user_id = Integer.parseInt(stu_data.get(student.KEY_ID));

        updateToken(user_id);

        roomArrayList = new ArrayList<>();
        initRecyclerView();

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Constants.PUSH_NOTIFICATION)){
                    int chat_id = Integer.parseInt(intent.getStringExtra("chat_id"));
                    String name = intent.getStringExtra("name");
                    String message = intent.getStringExtra("message");
                    String timestamp = intent.getStringExtra("timestamp");

                    for (ChatRoom rc : roomArrayList) {
                        if(rc.getId() == chat_id){
                            rc.setLastMessage(message);
                            rc.setUnreadCount(rc.getUnreadCount() + 1);
                            rc.setName(name);
                            rc.setTimestamp(timestamp);
                        }
                    }
                    adapter.notifyDataSetChanged();

                }else{
                    Toast.makeText(getActivity().getApplicationContext(), "", Toast.LENGTH_SHORT).show();
                }
            }
        };


        return view;
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getActivity().getApplicationContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
        adapter = new ChatRoomsAdapter(getActivity().getApplicationContext(),roomArrayList);
        recyclerView.setAdapter(adapter);
        getChatRooms(user_id);

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {

                Intent i = new Intent(getActivity(), ChatRoomActivity.class);
                i.putExtra("title",roomArrayList.get(position).getTitle());
                startActivity(i);
            }
        });

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

    private void getChatRooms(int user_id) {
        AppController.getApi().getChatRooms(1,"getChatRooms",user_id,0).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Rooms rooms = new Rooms(response);
                for (int i = 0; i < rooms.size(); i++) {
                    ChatRoom cr = new ChatRoom(rooms.getChatId(i),0,rooms.getTitle(i),rooms.getName(i),rooms.getTimestamp(i),"",rooms.getLastMessage(i));
                    roomArrayList.add(cr);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }

    private void updateToken(int user_id) {
        AppController.getApi().updateRegToken(1,"updateToken",FirebaseInstanceId.getInstance().getToken(),user_id,utype).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                new Success(response);
                Toast.makeText(getActivity().getApplicationContext(), success() ? "ok": "not ok", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }


}