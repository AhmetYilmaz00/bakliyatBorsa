package com.ans.borsa

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.auth.User
import kotlinx.android.synthetic.main.activity_ana_menu.*
import kotlinx.android.synthetic.main.activity_emirler.*

class emirler : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    // arrayler tanımlandı
    var urunMiktariFromFB: ArrayList<String> = ArrayList()
    var urunOnayiFromFB: ArrayList<String> = ArrayList()
    var urunFromFB: ArrayList<String> = ArrayList()
    var urunTutariFromFB: ArrayList<String> = ArrayList()
    var UserEmailFromFB: ArrayList<String> = ArrayList()
    var emirler: emirlerRA? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emirler)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        var layoutManagerEmiler = LinearLayoutManager(this)
        emirlerRV.layoutManager = layoutManagerEmiler
        emirler = emirlerRA(urunFromFB,urunOnayiFromFB,urunMiktariFromFB,UserEmailFromFB,urunTutariFromFB)
        emirlerRV.adapter = emirler
        getDataFromFSEmirler()
    }

    fun getDataFromFSEmirler() { //satın al bilgileri rv ye çekiliyor

        db.collection("Emirler").addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Toast.makeText(applicationContext, exception.localizedMessage.toString(), Toast.LENGTH_LONG).show()
            } else {
                if (snapshot != null) {
                    if (!snapshot.isEmpty) {
                        urunMiktariFromFB.clear()
                        urunTutariFromFB.clear()
                        UserEmailFromFB.clear()
                        urunFromFB.clear()
                        urunOnayiFromFB.clear()
                        var documents = snapshot.documents
                        for (document in documents) {
                            val urunKG = document.get("UrunKG") as Number
                            val urunTutari = document.get("EmirTutar") as Number
                            val userEmail = document.get("UserEmail") as String
                            val urun = document.get("Urun") as String
                            val urunOnay = document.get("EmirOnay") as Boolean

                            val urunMiktariText = urunKG.toString() + "KG"
                            val urunTutariText = urunTutari.toString() + "TL"
                            val urunOnayText = urunOnay.toString()

                                if(userEmail == auth.currentUser!!.email.toString()){
                                    UserEmailFromFB.add(userEmail)
                                    urunMiktariFromFB.add(urunMiktariText)
                                    urunTutariFromFB.add(urunTutariText)
                                    urunFromFB.add(urun)
                                    if(urunOnayText=="false"){
                                        urunOnayiFromFB.add("Emriniz Onaylanmadı")
                                    }else if(urunOnayText=="true"){
                                        urunOnayiFromFB.add("Emriniz Onaylandı")
                                        getDFSatilanUrunler(urunKG,urunTutari,userEmail,urun)
                                    }

                                    emirler!!.notifyDataSetChanged()
                                }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.options_menu_kullanici, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.logout) {
            auth.signOut()
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else if (item.itemId == R.id.anasayfa) {
            val intent = Intent(applicationContext, AnaMenu::class.java)
            startActivity(intent)
        }else if (item.itemId == R.id.bilgigiris) {
            val intent = Intent(applicationContext, com.ans.borsa.bilgiGiris::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }


    fun getDFSatilanUrunler(urunKG: Number,urunTutari: Number,userEmail :String,urun :String) {
        var y=0 //satın al bilgileri rv ye çekiliyor
        db.collection("KullaniciUrunleri").document("KullaniciUrunleriDoc")
                .collection(urun).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Toast.makeText(applicationContext, exception.localizedMessage.toString(), Toast.LENGTH_LONG).show()
            } else {
                if (snapshot != null) {
                    if (!snapshot.isEmpty) {

                        var documents = snapshot.documents
                        for (document in documents) {
                            val sUrunTutari = document.get("UrunTutari") as Number
                            val sUserEmail = document.get("UserEmail") as String
                            if(sUrunTutari==urunTutari && y==0){
                                y++
                                emirIsle(urun,sUserEmail,urunKG,urunTutari)
                            }

                        }
                    }
                }
            }
        }
    }

    fun emirIsle(secilenUrun: String, sUserEmail: String,urunKG: Number,urunTutari: Number) { // satın al butonuna basılırsa eğer bakiyede para varsa satın alma işlemi yapılıyor
        var y = 0
        var saticininKalanUrunu: Number
        var almakIstenenKG = urunKG.toDouble()

        db.collection("KullaniciUrunleri").document("KullaniciUrunleriDoc")
                .collection(secilenUrun).orderBy("UrunTutari", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        Toast.makeText(applicationContext, exception.localizedMessage.toString(), Toast.LENGTH_LONG).show()
                    } else {
                        if (snapshot != null) {
                            if (!snapshot.isEmpty) {
                                var documents = snapshot.documents

                                for (document in documents) {
                                    var urunMiktari = document.get("UrunMiktari") as Number
                                    var UserEmail = document.get("UserEmail") as String
                                    var urunMiktariD = urunMiktari.toDouble()

                                    if (almakIstenenKG <= urunMiktariD && y == 0  && sUserEmail==UserEmail) {
                                        y++
                                        saticininKalanUrunu = urunMiktariD - almakIstenenKG
                                        db.collection("KullaniciUrunleri").document("KullaniciUrunleriDoc")
                                                .collection(secilenUrun).document(document.id)
                                                .update("UrunMiktari", saticininKalanUrunu)
                                        emiriKaydet(urunKG,urunTutari,secilenUrun)
                                        break
                                    }

                                }
                            }
                        }
                    }
                }

    }


    fun emiriKaydet(urunKGKayit :Number,urunTutarKayit: Number,urunKayit: String){ // Emir verme işlemini yapan kod
        val postMapKayitUrun = hashMapOf<String, Any>()
        postMapKayitUrun.put("urunKGKayit", urunKGKayit)
        postMapKayitUrun.put("urunTutarKayit", urunTutarKayit)
        postMapKayitUrun.put("urunKayit", urunKayit)
        postMapKayitUrun.put("tarih", Timestamp.now()) //emir bilgileri veri tabanına aktarılıyor
        db.collection("UrunKayitleri").document(auth.currentUser!!.email.toString()).collection("SatinAlinanUrunler").add(postMapKayitUrun).addOnCompleteListener { task ->

            if (task.isComplete && task.isSuccessful) {
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(applicationContext, exception.localizedMessage.toString(), Toast.LENGTH_LONG).show()
        }
    }


}