package jp.techacademy.yasuhiko.tokushima.qa_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AndroidException;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private int mGenre = 0;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private DatabaseReference mFavoriteRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private ArrayList<Favorite> mFavoriteArrayList;
    private QuestionListAdapter mAdapter;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
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
                            mGenre,
                            bytes,
                            answerArrayList
                    );
            mQuestionArrayList.add(question);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // 変更があったQuestionを返す
            for (Question question: mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答（answer）のみ
                    question.getAnswers().clear();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            question.getAnswers().add(answer);
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }
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

    private ChildEventListener mEventListenerFav = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String favKey = dataSnapshot.getKey();
            String favoriteqid = (String) map.get("favoriteqid");
            Favorite favorite = new Favorite(favKey, favoriteqid);

            mFavoriteArrayList.add(favorite);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            // お気に入りは変更されないので何も処理はなし
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String favoriteqid = (String) map.get("favoriteqid");

            for (int i = 0; i < mFavoriteArrayList.size(); i++) {
                if (mFavoriteArrayList.get(i).getmFavoriteQid().equals(favoriteqid)) {
                    mFavoriteArrayList.remove(i);
                    return;
                }
            }
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
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
                if (mGenre == 0) {
                    Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show();
                    return;
                }

                // ログイン済みのユーザを収録する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                // ログインしていなければ、ログイン画面に遷移させる（ログインしていないときはnullが返る）
                if (user == null) {
                    // ログインしていなければ、ログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // ジャンルを渡して質問作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                    intent.putExtra("genre", mGenre);
                    startActivity(intent);
                }
            }
        });

        // ナビゲーションドロワーの設定
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_hobby) {
                    mToolbar.setTitle("趣味");
                    mGenre = 1;
                } else if (id == R.id.nav_life) {
                    mToolbar.setTitle("生活");
                    mGenre = 2;
                } else if (id == R.id.nav_health) {
                    mToolbar.setTitle("健康");
                    mGenre = 3;
                } else if (id == R.id.nav_computer) {
                    mToolbar.setTitle("コンピューター");
                    mGenre = 4;
                } else if (id == R.id.nav_favorite) {
                    // お気に入り一覧画面へ遷移する
                    Intent intent = new Intent(getApplicationContext(), FavQListActivity.class);
                    intent.putExtra("favList", mFavoriteArrayList);
                    startActivity(intent);
                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);

                // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListviewにセットし直す
                mQuestionArrayList.clear();
                mAdapter.setmQuestionArrayList(mQuestionArrayList);
                mListView.setAdapter(mAdapter);

                // 選択したジャンルにリスナーを登録する
                if (mGenreRef != null) {
                    mGenreRef.removeEventListener(mEventListener);
                }
                mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
                mGenreRef.addChildEventListener(mEventListener);

                return true;
            }
        });

        // ログインしているか判定して「お気に入り」の表示／非表示をコントロールする
        Menu menu = (Menu) navigationView.getMenu();
        MenuItem menu_fav = (MenuItem) menu.getItem(4);

        // ログイン済みのユーザを収録する
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // ログインしている場合は、お気に入りメニューを表示する
        if (user == null) {
            menu_fav.setVisible(false);
        } else {
            menu_fav.setVisible(true);
        }

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // Listviewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

        // ログインしているユーザのFavoriteのリスナーを登録する
        if (user != null) {
            mFavoriteRef = mDatabaseReference.child(Const.FavoritePATH).child(user.getUid());
            mFavoriteRef.addChildEventListener(mEventListenerFav);
        }
        // mFavoriteArrayListの初期化
        mFavoriteArrayList = new ArrayList<Favorite>();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // この画面に戻ってきたら（主にログイン、ログアウトの状態が変わったら）ログイン状態に
        // よるドロワーの状態変更とお気に入りデータのクリア等の処理を行う

        // ログインしているか判定して「お気に入り」の表示／非表示をコントロールする
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = (Menu) navigationView.getMenu();
        MenuItem menu_fav = (MenuItem) menu.getItem(4);

        // ログイン済みのユーザを収録する
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // ログインしている場合は、お気に入りメニューを表示する
        if (user == null) {
            menu_fav.setVisible(false);

            // お気に入りデータをクリア
            if (mFavoriteRef != null) {
                mFavoriteRef.removeEventListener(mEventListenerFav);
            }
            mFavoriteArrayList.clear();
        } else {
            menu_fav.setVisible(true);

            // お気に入りデータをクリアして再取得
            if (mFavoriteRef != null) {
                mFavoriteRef.removeEventListener(mEventListenerFav);
            }
            mFavoriteArrayList.clear();
            mFavoriteRef = mDatabaseReference.child(Const.FavoritePATH).child(user.getUid());
            mFavoriteRef.addChildEventListener(mEventListenerFav);
        }
    }
}
