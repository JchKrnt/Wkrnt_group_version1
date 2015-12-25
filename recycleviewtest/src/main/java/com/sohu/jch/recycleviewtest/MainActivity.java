package com.sohu.jch.recycleviewtest;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    private Button addbtn;
    private RecyclerView review;
    private MyViewAdapter adapter;
    private Button removeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    private void initialize() {

        addbtn = (Button) findViewById(R.id.add_btn);
        removeBtn = (Button) findViewById(R.id.remove_btn);
        review = (RecyclerView) findViewById(R.id.re_view);
        adapter = new MyViewAdapter(getApplicationContext());

        review.setAdapter(adapter);
        review.setHasFixedSize(true);
        review.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        review.setItemAnimator(new DefaultItemAnimator());


        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String contentStr = "item " + adapter.getData().size();
                adapter.addItemData(adapter.getData().size(), contentStr);
            }
        });

        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int randomId = new Random().nextInt(adapter.getData().size());
                adapter.removeItem(randomId);
            }
        });

    }

    class MyViewAdapter extends RecyclerView.Adapter<MyViewAdapter.MViewHolder> {

        private Context context;
        private ArrayList<String> data = new ArrayList<>();
        private TextView itemtv;

        public MyViewAdapter(Context context) {
            this.context = context;
        }

        @Override
        public void onBindViewHolder(MViewHolder holder, int position) {

            holder.textView.setText(data.get(position));
        }

        @Override
        public MViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false);

            MViewHolder viewHolder = new MViewHolder(view);
            return viewHolder;
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public ArrayList<String> getData() {
            return data;
        }

        public void addItemData(int position, String content) {

            data.add(content);
            notifyItemInserted(position);

        }

        public void removeItem(int position) {

            data.remove(position);
            notifyItemRemoved(position);
        }

        public class MViewHolder extends RecyclerView.ViewHolder {

            View containerView;

            TextView textView;

            public MViewHolder(View itemView) {
                super(itemView);
                containerView = itemView;
                textView = (TextView) containerView.findViewById(R.id.item_tv);
            }
        }
    }
}
