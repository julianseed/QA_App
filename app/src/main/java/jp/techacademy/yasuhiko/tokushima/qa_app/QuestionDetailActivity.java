package jp.techacademy.yasuhiko.tokushima.qa_app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class QuestionDetailActivity extends AppCompatActivity
        implements View.OnClickListener, DatabaseReference.CompletionListener {
    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;
    private boolean mFavoriteFlag;
    private String mFavKey;
    private ProgressDialog mProgress;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mAnswerRef;
    private FirebaseUser mUser;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for (Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
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
        setContentView(R.layout.activity_question_detail);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");
        mFavoriteFlag = extras.getBoolean("favoriteflag");
        mFavKey = extras.getString("favkey");

        setTitle(mQuestion.getTitle());

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("");

        Button favoriteButton = (Button) findViewById(R.id.favorite);

        // Firebaseの準備
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        // ログイン済みのユーザを収録する
        mUser = FirebaseAuth.getInstance().getCurrentUser();

        if(mUser == null) {
            favoriteButton.setVisibility(View.INVISIBLE);
        } else {
            if (mFavoriteFlag) {
                favoriteButton.setBackgroundResource(R.drawable.btn_pressed);
                favoriteButton.setText("お気に入りから解除");
            } else {
                favoriteButton.setBackgroundResource(R.drawable.btn);
                favoriteButton.setText("お気に入りに登録");
            }
            favoriteButton.setOnClickListener(this);
            favoriteButton.setVisibility(View.VISIBLE);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUser == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // Questionを渡して回答作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                }
            }
        });

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = databaseReference.child(Const.ContentsPATH)
                .child(String.valueOf(mQuestion.getGenre()))
                .child(mQuestion.getQuestionUid())
                .child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);
    }

    @Override
    public void onClick(View v) {
        if (mUser != null) {
            if (mFavoriteFlag) {
                mFavoriteFlag = false;
                Button fv = (Button) v.findViewById(R.id.favorite);
                fv.setBackgroundResource(R.drawable.btn);
                fv.setText("お気に入りに登録");

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                if (mUser != null) {
                    DatabaseReference mFavoriteRef =
                            databaseReference.child(Const.FavoritePATH)
                                    .child(mUser.getUid())
                                    .child(mFavKey);

                    mFavoriteRef.removeValue(this);

                    mProgress.setMessage("お気に入りから解除中");
                    mProgress.show();
                }
            } else {
                mFavoriteFlag = true;
                Button fv = (Button) v.findViewById(R.id.favorite);
                fv.setBackgroundResource(R.drawable.btn_pressed);
                fv.setText("お気に入りから解除");

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                if (mUser != null) {
                    DatabaseReference mFavoriteRef =
                            databaseReference.child(Const.FavoritePATH)
                                    .child(mUser.getUid());

                    Map<String, String> data = new HashMap<String, String>();

                    // QID
                    data.put("favoriteqid", mQuestion.getQuestionUid());

                    mFavoriteRef.push().setValue(data, this);

                    mProgress.setMessage("お気に入りに登録中");
                    mProgress.show();
                }
            }
        }
    }

    @Override
    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        mProgress.dismiss();

        if (databaseError == null) {
            //-=finish();
        } else {
            if (mFavoriteFlag == true) {
                // フラグがtrueということは、登録中→解除処理の後
                Snackbar.make(findViewById(android.R.id.content), "お気に入りの解除に失敗しました", Snackbar.LENGTH_LONG).show();
            } else {
                // フラグがfalseということは、解除→登録中処理の後
                Snackbar.make(findViewById(android.R.id.content), "お気に入りの登録に失敗しました", Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
