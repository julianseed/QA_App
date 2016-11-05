package jp.techacademy.yasuhiko.tokushima.qa_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class FavQListActivity extends AppCompatActivity {
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mQuestionRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private ArrayList<Favorite> mFavoriteArrayList;
    private FavQuestionListAdapter mAdapter;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // お気に入りに登録されているものだけ処理する
            for (Favorite favorite: mFavoriteArrayList) {
                if (dataSnapshot.getKey().equals(favorite.getmFavoriteQid())) {
                    String title = (String) map.get("title");
                    String body = (String) map.get("body");
                    String name = (String) map.get("name");
                    String uid = (String) map.get("uid");
                    String imageString = (String) map.get("image");
                    Bitmap image = null;
                    byte[] bytes;
                    if (imageString != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        bytes = Base64.decode(imageString, Base64.DEFAULT);
                    } else {
                        bytes = new byte[0];
                    }

                    ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            answerArrayList.add(answer);
                        }
                    }

                    Question question =
                            new Question(
                                    title,
                                    body,
                                    name,
                                    uid,
                                    dataSnapshot.getKey(),
                                    0,      // この画面以降ではジャンルを使わないので、固定で入れる
                                    bytes,
                                    answerArrayList
                            );
                    mQuestionArrayList.add(question);
                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_qlist);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mFavoriteArrayList = (ArrayList<Favorite>) extras.getSerializable("favList");

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // Listviewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new FavQuestionListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

        mAdapter.setmFavQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);

        // 選択したジャンルにリスナーを登録する
        if (mQuestionRef != null) {
            mQuestionRef.removeEventListener(mEventListener);
        }
        mQuestionRef = mDatabaseReference.child(Const.ContentsPATH).child("1");
        mQuestionRef.addChildEventListener(mEventListener);
        mQuestionRef = mDatabaseReference.child(Const.ContentsPATH).child("2");
        mQuestionRef.addChildEventListener(mEventListener);
        mQuestionRef = mDatabaseReference.child(Const.ContentsPATH).child("3");
        mQuestionRef.addChildEventListener(mEventListener);
        mQuestionRef = mDatabaseReference.child(Const.ContentsPATH).child("4");
        mQuestionRef.addChildEventListener(mEventListener);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                // お気に入りに登録されているかを送る
                boolean bFavorite = false;
                String favKey = "";
                if (mFavoriteArrayList != null) {
                    for (Favorite favorite : mFavoriteArrayList) {
                        if (mQuestionArrayList.get(position)
                                .getQuestionUid()
                                .equals(favorite.getmFavoriteQid())) {
                            bFavorite = true;
                            favKey = favorite.getmFavKey();
                            break;
                        }
                    }
                }
                intent.putExtra("favoriteflag", bFavorite);
                intent.putExtra("favkey", favKey);
                startActivity(intent);
            }
        });
    }
}
