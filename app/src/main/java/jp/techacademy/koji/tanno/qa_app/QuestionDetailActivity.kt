package jp.techacademy.koji.tanno.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_question_detail.*


class QuestionDetailActivity : AppCompatActivity(), DatabaseReference.CompletionListener  {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    private var isFavorite: Boolean = false             //　追加

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>
            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            TODO("Not yet implemented")
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            TODO("Not yet implemented")
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済のユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }

            val mDataBaseReference = FirebaseDatabase.getInstance().reference
            mAnswerRef = mDataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
                .child(mQuestion.questionUid).child(AnswersPATH)
            mAnswerRef.addChildEventListener(mEventListener)


        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_question_detail,menu)

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        // ログインしていればお気に入りボタンを表示する
        if (user != null) {

            //QuestionUid所得
            val questionUid = mQuestion.questionUid

            // ユーザーがお気に入りに登録しているか確認
            val mDataBaseReference = FirebaseDatabase.getInstance().reference
            val mFavoritesRef = mDataBaseReference.child(UsersPATH).child(user.uid).child(FavoritesPATH)

            mFavoritesRef.addListenerForSingleValueEvent(object: ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val favoriteMap = snapshot.value as Map<String,String>?

                    if (favoriteMap != null) {
                        for (favorite in favoriteMap.keys) {
                            val temp = favoriteMap[favorite] as Map<String, String>
                            val favoriteQuestionUid = temp["questionUid"] ?: ""
                            if (favoriteQuestionUid == questionUid) {
                                // お気に入りに登録済みの場合
                                isFavorite = true
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

            // お気に入りボタンのアイコンを切り替える
            if (isFavorite) {
                menu?.findItem(R.id.favorite)
                    ?.setIcon(R.drawable.ic_star)
            } else {
                menu?.findItem(R.id.favorite)
                    ?.setIcon(R.drawable.ic_star_border)
            }

        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.favorite -> {

                // ログイン済みのユーザーを取得する
                val user = FirebaseAuth.getInstance().currentUser

                // Firebaseのお気に入り情報を更新
                if (user != null) {
                    val questionUid = mQuestion.questionUid
                    val mDataBaseReference = FirebaseDatabase.getInstance().reference
                    val mFavoritesRef =
                        mDataBaseReference.child(UsersPATH).child(user.uid).child(FavoritesPATH)
                    if (isFavorite) {
                        // お気に入り登録済みなら、削除
                        item.setIcon(R.drawable.ic_star_border)
                        progressBar.visibility = View.VISIBLE
                        mFavoritesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val favoriteMap = snapshot.value as Map<String, String>?
                                if (favoriteMap != null) {
                                    for (favorite in favoriteMap.keys) {
                                        val temp = favoriteMap[favorite] as Map<String, String>
                                        val favoriteQuestionUid = temp["questionUid"] ?: ""
                                        if (favoriteQuestionUid == questionUid) {
                                            // お気に入りに登録済みの場合
                                            mFavoritesRef.child(favorite).removeValue()
                                            isFavorite = false

                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "お気に入り更新に失敗しました：" + error!!,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        })
                        if (!isFavorite) {

                        } else {
                            isFavorite = false
                        }

                    } else {
                        // お気に入り登録未済なら、登録
                        val data = HashMap<String, String>()
                        data["questionUid"] = questionUid
                        progressBar.visibility = View.VISIBLE
                        mFavoritesRef.push().setValue(data, this)
                        item.setIcon(R.drawable.ic_star)
                        isFavorite = true
                    }

                    return true
                }
            }
        }
        return true
    }

    override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (error == null) {
            // 何もしない
        } else {
            Snackbar.make(findViewById(android.R.id.content), "お気に入り更新に失敗しました：" + error!!, Snackbar.LENGTH_LONG).show()
        }

    }


}

