package com.example.gaia

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit



class SessionActivity : AppCompatActivity() {
    private lateinit var username: String
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    private lateinit var sessionTextView: TextView

    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var endSessionButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)

        // Receber o username
        username = intent.getStringExtra("USERNAME") ?: ""

        // Inicializar views
        messageEditText = findViewById(R.id.user_input)
        sendButton = findViewById(R.id.send_button)
        sessionTextView = findViewById(R.id.session_text_view)
        endSessionButton = findViewById(R.id.end_session_button) // Inicialize o botão de encerrar sessão



        // Chamar a função para verificar ou criar o histórico
        checkOrCreateUserHistory(username)

        // Configurar o botão de enviar
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToGaia(username, message)
                messageEditText.text.clear() // Limpa o campo de entrada
            } else {
                Toast.makeText(this, "Por favor, insira uma mensagem.", Toast.LENGTH_SHORT).show()
            }
        }
        endSessionButton.setOnClickListener {
            finish()
        }
    }

    private fun checkOrCreateUserHistory(username: String) {
        Log.d("SessionActivity", "Verificando ou criando histórico para o usuário: '$username'")
        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url("http://10.0.2.2:5000/check_or_create_history?username=$username")
                .get()
                .build()


            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@SessionActivity, "Erro ao verificar ou criar histórico: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        // O histórico foi verificado ou criado com sucesso
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SessionActivity, "Erro ao verificar ou criar histórico: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    private fun sendMessageToGaia(username: String, message: String) {
        val jsonBody = JSONObject().apply {
            put("username", username)
            put("message", message)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), jsonBody.toString())

            val request = Request.Builder()
                .url("http://10.0.2.2:5000/gaia")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@SessionActivity,
                            "Erro ao enviar mensagem: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (response.isSuccessful) {
                            val jsonResponse = response.body?.string()
                            Log.d("SessionActivity", "Received JSON Response: $jsonResponse")

                            try {
                                val responseObject = JSONObject(jsonResponse)
                                val aiResponse = responseObject.getString("response")

                                runOnUiThread {
                                    sessionTextView.append("Você: $message\n")
                                    sessionTextView.append("$aiResponse\n")
                                }
                            } catch (e: Exception) {
                                Log.e("SessionActivity", "Error parsing JSON: ${e.message}")
                                runOnUiThread {
                                    Toast.makeText(this@SessionActivity, "Erro ao processar a resposta.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@SessionActivity,
                                    "Erro: ${response.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            })
        }
    }
}

