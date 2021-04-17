package com.android.yaho

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import com.android.yaho.base.BindingActivity
import com.android.yaho.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class LoginActivity : BindingActivity<ActivityLoginBinding>(ActivityLoginBinding::inflate){
    private val TAG = this::class.simpleName

    private val firebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private lateinit var verificationId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.progressCircular.isVisible = true
        firebaseAuth.currentUser?.let { user ->
            binding.progressCircular.isVisible = false
            Log.d(TAG, "firebaseAuth UID  : ${user.uid}")
            finish()
        } ?: run {
            binding.progressCircular.isVisible = false
            binding.etPhoneNumber.setText("+1 650-555-3535")
        }

        initView()
    }

    private fun initView() {

        binding.btnVerification.setOnClickListener {
            when(binding.btnVerification.text) {
                "인증번호 받기" -> verifyPhoneNumber(binding.etPhoneNumber.text.toString())
                "인증번호 입력" -> {
                    val code = binding.etCode.text.toString()
                    val credential = PhoneAuthProvider.getCredential(verificationId, code)
                    signInWithPhoneAuthCredential(credential)
                }
            }
        }
    }

    private fun verifyPhoneNumber(phoneNumber: String) {
        Log.d(TAG, "try verifyPhoneNumber:$phoneNumber")
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // 사용자의 전화번호가 정상적으로 인증된 것이므로
                    // 콜백에 전달된 PhoneAuthCredential 객체를 사용하여 사용자를 로그인 처리할 수 있습니다.
                    Log.d(TAG, "onVerificationCompleted:$credential")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // 요청에 잘못된 전화번호 또는 인증 코드가 지정된 경우와 같이
                    // 잘못된 인증 요청에 대한 응답으로 호출됩니다.
                    Log.w(TAG, "onVerificationFailed", e)

                    if (e is FirebaseAuthInvalidCredentialsException) {
                        // Invalid request
                        Toast.makeText(this@LoginActivity, "Invalid request", Toast.LENGTH_SHORT).show()
                    } else if (e is FirebaseTooManyRequestsException) {
                        // The SMS quota for the project has been exceeded
                        Toast.makeText(this@LoginActivity, "SMS 할당량이 초과되었습니다.", Toast.LENGTH_SHORT).show()
                    }

                    // Show a message and update the UI
                    Toast.makeText(this@LoginActivity, "onVerificationFailed", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    // The SMS verification code has been sent to the provided phone number, we
                    // now need to ask the user to enter the code and then construct a credential
                    // by combining the code with a verification ID.
                    Log.d(TAG, "onCodeSent:$verificationId")
                    this@LoginActivity.verificationId = verificationId
                    // Save verification ID and resending token so we can use them later
//                    storedVerificationId = verificationId
//                    resendToken = token
//                    Toast.makeText(this@LoginActivity, "onCodeSent", Toast.LENGTH_SHORT).show()
                    binding.btnVerification.text = "인증번호 입력"
                    binding.etCode.setText("654321")
                }
            })          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    val userUid = task.result?.user?.uid
                    Toast.makeText(this@LoginActivity, "Success : $userUid", Toast.LENGTH_SHORT).show()
                    userUid?.let { getUserDataFromFirestore(userUid) }
                    finish()
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(this@LoginActivity, "verification code invalid : ${task.exception}", Toast.LENGTH_SHORT).show()
                    }
                    // Update UI
                    Toast.makeText(this@LoginActivity, "Error", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getUserDataFromFirestore(uid:String) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "result.data => ${result.data}")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
                updateNewUserToFirestore(uid)
            }
    }

    private fun updateNewUserToFirestore(uid:String) {
        db.collection("users")
            .document(uid)
            .set(UserClimbingData())
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }
}

data class UserClimbingData(
    val allHeight: Float = 0f,
    val allDistance: Float = 0f,
    val allTime: Int = 0,
    val records: List<ClimbingRecordData> = emptyList()
)

data class ClimbingRecordData(
    val mountainId: Int = 0,
    val height: Float = 0f,
    val distance: Float = 0f,
    val runningTime: Int = 0,
)
