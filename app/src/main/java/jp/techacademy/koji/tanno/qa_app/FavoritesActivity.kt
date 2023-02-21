package jp.techacademy.koji.tanno.qa_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_favorites.*


class FavoritesActivity : AppCompatActivity(), DatabaseReference.CompletionListener {


    private lateinit var mAdapter: FavoritesListAdapter
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private var mFavoriteQidArrayList = ArrayList<String>()

    private val mFavoritesEventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val favoriteMap = snapshot.value as Map<String, String>
            mFavoriteQidArrayList.clear()
            for (favorite in favoriteMap.keys) {
                if (favoriteMap[favorite] != null) {
                    mFavoriteQidArrayList.add(favoriteMap[favorite]!!)
                    Log.v("DEBUG","FAVORITES = " + favoriteMap[favorite] ?: "")
                }
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onCancelled(error: DatabaseError) {
        }

    }

    private var mQuestionsEventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

            val genreMap = snapshot.value as Map<String, String>
            for (questionUid in genreMap.keys) {
                val questionMap = genreMap[questionUid] as Map<String, String>?
                if (questionMap != null) {
                    val title = questionMap["title"] ?: ""
                    val body = questionMap["body"] ?: ""
                    val name = questionMap["name"] ?: ""
                    val uid = questionMap["uid"] ?: ""
                    val imageString = questionMap["image"] ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }
                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = questionMap["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            answerArrayList.add(answer)
                        }
                    }
                    val question = Question(
                        title, body, name, uid, questionUid, 0, bytes, answerArrayList
                    )

                    if (mFavoriteQidArrayList.contains(questionUid)) {
                        mQuestionArrayList.add(question)
                        mAdapter.setQuestionArrayList(mQuestionArrayList)
                        mAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            TODO("Not yet implemented")
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            TODO("Not yet implemented")
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            TODO("Not yet implemented")
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }


    }


    override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
        if (error == null) {

        } else {
            Snackbar.make(findViewById(android.R.id.content), "DB更新に失敗しました：" + error!!, Snackbar.LENGTH_LONG).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        title = "お気に入り"

        // ListViewの準備
        mAdapter = FavoritesListAdapter(this)
        favoritesListView.adapter = mAdapter




    }

    override fun onResume() {
        super.onResume()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {

            val mDatabaseReference = FirebaseDatabase.getInstance().reference

            // お気に入りのQuestionIDの配列を用意
            val mFavoritesRef =
                mDatabaseReference.child(UsersPATH).child(user.uid).child(FavoritesPATH)
            mFavoritesRef.addChildEventListener(mFavoritesEventListener)

            // お気に入りのQuestionの配列を用意
            mQuestionArrayList = ArrayList<Question>()
            val mContentsRef = mDatabaseReference.child(ContentsPATH)
            mContentsRef.addChildEventListener(mQuestionsEventListener)
        }
        mAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        mQuestionArrayList = ArrayList<Question>()

        val mDatabaseReference = FirebaseDatabase.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser

        // お気に入りのQuestionIDの配列を用意
        val mFavoritesRef =
            mDatabaseReference.child(UsersPATH).child(user!!.uid).child(FavoritesPATH)
        mFavoritesRef.removeEventListener(mFavoritesEventListener)

        // お気に入りのQuestionの配列を用意
        val mContentsRef = mDatabaseReference.child(ContentsPATH)
        mContentsRef.removeEventListener(mQuestionsEventListener)


    }

}