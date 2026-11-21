package com.wesee.app.ui.screens.pcd.chat

import kotlin.random.Random

/** Sugestão do chat. [widthWeight] = largura relativa do card na linha (o grid é assimétrico); [textSizeSp] = fonte proporcional ao card. */
data class ChatSuggestion(
    val id: String,
    val title: String,
    val replies: List<String>,
    val widthWeight: Float,
    val textSizeSp: Int,
)

data class ChatMessage(val id: Long, val text: String, val fromUser: Boolean)

/**
 * TODAS as sugestões e respostas do Navi (100% local, sem IA e sem rede). Cada sugestão tem 5 respostas.
 * As respostas não inventam localização, clima, rotas, compromissos nem anotações: só orientam sobre os recursos do app.
 */
object NaviChatData {

    val suggestions: List<ChatSuggestion> = listOf(
        ChatSuggestion(
            id = "onde_estou", title = "Onde estou?", widthWeight = 0.27f, textSizeSp = 12,
            replies = listOf(
                "Para saber onde você está, abra a aba Maps: ela mostra a sua posição no mapa assim que a localização do celular estiver ligada.",
                "Ainda não consigo dizer o seu endereço por aqui, mas a aba Maps mostra a sua posição atual. Confira se a localização do celular está ativada.",
                "Quer se orientar? Ative a localização do celular e toque em Maps. Lá você vê onde está e pode buscar um destino a partir dali.",
                "Em um lugar novo, a aba Maps é uma boa companheira: ela marca a sua posição e ajuda a escolher para onde ir. Só lembre de permitir a localização.",
                "Vamos com calma: primeiro confira se a localização está habilitada, depois toque em Maps. Na tela Home, a câmera também descreve por voz o que está à sua frente.",
            ),
        ),
        ChatSuggestion(
            id = "tempo", title = "Como está o tempo?", widthWeight = 0.34f, textSizeSp = 13,
            replies = listOf(
                "Eu não consigo consultar o clima em tempo real por aqui. Antes de sair, vale abrir o app de previsão do tempo do seu celular.",
                "Para saber se vai chover ou fazer frio, consulte a previsão no seu celular. Uma olhada rápida antes de sair ajuda a se preparar.",
                "Ainda não tenho acesso à previsão do tempo. Que tal perguntar ao assistente de voz do celular ou abrir o app de clima antes de sair?",
                "Não quero te dar uma previsão errada, então prefiro não chutar. Confira a previsão atualizada no celular e leve o que precisar: guarda-chuva, casaco ou água.",
                "Dica de segurança: em dia de chuva o chão fica escorregadio. Consulte a previsão no seu celular antes de sair e, se precisar, vá com mais calma.",
            ),
        ),
        ChatSuggestion(
            id = "rota", title = "Me ajude com uma rota.", widthWeight = 0.39f, textSizeSp = 13,
            replies = listOf(
                "Vamos lá! Abra a aba Maps, digite o destino no campo Destino e escolha uma das sugestões. O app mostra a rota, a distância e o tempo a pé.",
                "Para traçar um caminho, use a aba Maps: escolha o destino e confira a distância e o tempo estimado antes de sair.",
                "Planejar antes ajuda muito: veja o trajeto na aba Maps com calma e, se puder, avise alguém de confiança por onde você vai.",
                "Na aba Maps você busca o endereço ou o lugar que quer visitar. A rota sai da sua posição, então mantenha a localização do celular ligada.",
                "Se o lugar é novo para você, olhe o trajeto na aba Maps com antecedência e preste atenção em calçadas, travessias e obstáculos pelo caminho.",
            ),
        ),
        ChatSuggestion(
            id = "hoje", title = "O que tenho para hoje?", widthWeight = 0.37f, textSizeSp = 13,
            replies = listOf(
                "Ainda não tenho acesso à sua agenda. Mas você pode registrar lembretes no card Anotações da tela Home e conferir tudo no Mural de anotações.",
                "Que tal organizar o seu dia? Na tela Home, toque no card Anotações e escreva o que não pode esquecer. Depois é só olhar o mural.",
                "Não conheço os seus compromissos, e tudo bem: dá para anotá-los na Home. Escreva com calma e destaque o que for mais importante.",
                "Uma boa ideia é listar as tarefas do dia logo cedo. Use as anotações da Home e escolha uma cor para cada tipo de lembrete.",
                "Posso ajudar você a se organizar: pense no que precisa fazer hoje, em que ordem, e registre cada item nas anotações da tela Home.",
            ),
        ),
        ChatSuggestion(
            id = "anotacoes", title = "Ler minhas anotações", widthWeight = 0.32f, textSizeSp = 12,
            replies = listOf(
                "Suas anotações ficam no Mural de anotações, na tela Home. Toque em Home e role até o mural para vê-las.",
                "Ainda não leio as anotações em voz alta aqui no chat, mas você as encontra na tela Home, no Mural de anotações.",
                "Para criar uma nova, toque no card Anotações, na Home. Para rever as anteriores, olhe o Mural de anotações logo abaixo.",
                "Quer achar suas anotações? Vá até a tela Home e deslize o Mural para o lado: cada recado aparece em um cartãozinho colorido.",
                "Suas anotações ficam na tela Home, no Mural. Se ainda não escreveu nenhuma, toque no card Anotações para criar a primeira.",
            ),
        ),
        ChatSuggestion(
            id = "orientacao", title = "Me dê uma orientação", widthWeight = 0.31f, textSizeSp = 12,
            replies = listOf(
                "Claro! Me conta: você precisa de ajuda para se localizar, planejar um trajeto ou organizar o seu dia? Posso indicar o caminho dentro do app.",
                "Estou aqui para ajudar. Na Home a câmera descreve por voz o que está à sua frente, e na aba Maps você planeja trajetos. Por onde quer começar?",
                "Uma orientação importante: antes de sair, confira se a bateria do celular está boa e se a localização está ligada. Assim o WeSee funciona melhor.",
                "Se quiser explorar um lugar novo, planeje o trajeto na aba Maps e caminhe com calma. Qualquer dúvida, volte aqui.",
                "Respire fundo e vá no seu ritmo. Use a câmera da Home para ouvir o que está à frente e a aba Maps para saber para onde seguir.",
            ),
        ),
    )

    /** Seis cards em duas linhas de três (larguras diferentes em cada linha). */
    val rows: List<List<ChatSuggestion>> = suggestions.chunked(3)
}

/**
 * Escolhe a resposta de cada sugestão de forma INDEPENDENTE: sorteia sem reposição (todas as 5 respostas saem antes de
 * qualquer repetição) e nunca repete imediatamente a última resposta daquele card, nem na virada de um ciclo para o outro.
 */
class ReplyRotator(private val random: Random = Random.Default) {
    private val bags = mutableMapOf<String, ArrayDeque<Int>>()
    private val last = mutableMapOf<String, Int>()

    fun next(suggestion: ChatSuggestion): String {
        val size = suggestion.replies.size
        require(size > 0) { "A sugestão '${suggestion.id}' não tem respostas" }
        val bag = bags.getOrPut(suggestion.id) { ArrayDeque() }
        if (bag.isEmpty()) {
            val fresh = (0 until size).shuffled(random).toMutableList()
            val previous = last[suggestion.id]
            if (size > 1 && fresh.first() == previous) { // evita repetir a última na virada do ciclo
                val swap = 1 + random.nextInt(size - 1)
                fresh[0] = fresh[swap].also { fresh[swap] = fresh[0] }
            }
            bag.addAll(fresh)
        }
        val index = bag.removeFirst()
        last[suggestion.id] = index
        return suggestion.replies[index]
    }
}
