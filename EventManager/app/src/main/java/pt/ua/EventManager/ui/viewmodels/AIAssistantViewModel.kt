package pt.ua.EventManager.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.ua.EventManager.data.Event

class AIAssistantViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AIAssistantUiState>(AIAssistantUiState.Idle)
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    // Chave API atualizada que forneceu
    private val apiKey = "AIzaSyA_Xj4dQBSwQjbdp1KwQYS-acjwBJuPgOM".trim()
    
    private val generativeModel = GenerativeModel(
        // Usamos gemini-1.5-flash que é o modelo padrão mais atual e rápido
        modelName = "gemini-2.5-flash-lite",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
        }
    )

    fun getSuggestions(event: Event) {
        viewModelScope.launch {
            _uiState.value = AIAssistantUiState.Loading
            try {
                if (apiKey.isEmpty() || !apiKey.startsWith("AIza")) {
                    _uiState.value = AIAssistantUiState.Error("A chave API é inválida. Use uma chave do AI Studio (AIza...).")
                    return@launch
                }

                val prompt = """
                    Atua como um assistente especialista em planeamento de eventos em Portugal. 
                    Evento: "${event.title}" em "${event.address}".
                    Participantes: ${event.participantsUids.size} pessoas.
                    
                    Fornece sugestões detalhadas em Português de Portugal para:
                    1. Quantidades de comida e bebida recomendadas para este número de pessoas.
                    2. Orçamento estimado total com base nos preços médios atuais de supermercados em Portugal (ex: Continente, Pingo Doce, Lidl).
                    3. Três dicas essenciais de anfitrião para garantir o sucesso deste evento.
                    
                    Mantém a resposta concisa, profissional e formatada com pontos (bullet points).
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                _uiState.value = AIAssistantUiState.Success(response.text ?: "Não foi possível gerar sugestões.")
                
            } catch (e: Exception) {
                Log.e("AIAssistant", "Erro Gemini: ${e.message}", e)
                val errorMsg = e.localizedMessage ?: "Erro desconhecido"
                
                if (errorMsg.contains("404")) {
                    _uiState.value = AIAssistantUiState.Error(
                        "Erro 404: Modelo não encontrado.\n\n" +
                        "RESOLUÇÃO:\n" +
                        "1. Verifique se a 'Generative Language API' está ATIVA no seu console da Google Cloud.\n" +
                        "2. Confirme se criou a chave em aistudio.google.com.\n" +
                        "3. Tente novamente em alguns minutos."
                    )
                } else {
                    _uiState.value = AIAssistantUiState.Error("Falha na AI: $errorMsg")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AIAssistantUiState.Idle
    }
}

sealed class AIAssistantUiState {
    object Idle : AIAssistantUiState()
    object Loading : AIAssistantUiState()
    data class Success(val suggestions: String) : AIAssistantUiState()
    data class Error(val message: String) : AIAssistantUiState()
}
