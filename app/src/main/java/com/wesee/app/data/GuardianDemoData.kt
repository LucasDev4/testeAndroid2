package com.wesee.app.data

data class Responsavel(val nome: String)
/** Pessoa acompanhada, como as telas do Responsável a exibem. latitude/longitude = null quando ainda não há localização. */
data class PessoaAcompanhada(
    val nome: String, val endereco: String, val ultimaAtualizacao: String,
    val latitude: Double?, val longitude: Double?,
)
data class Dispositivo(val status: String, val bateria: Int, val localizacaoAtivada: Boolean, val iaAtivada: Boolean)
data class Alerta(val id: Int, val tipo: String, val titulo: String, val descricao: String, val horario: String)
data class Notificacao(val id: Int, val mensagem: String, val lida: Boolean = false)

/** Tudo que as telas do Responsável exibem. */
data class GuardianData(
    val responsavel: Responsavel, val pessoa: PessoaAcompanhada, val dispositivo: Dispositivo,
    val alertas: List<Alerta>, val notificacoes: List<Notificacao>,
)

object TipoAlerta {
    const val ROTA_DIFERENTE = "rota_diferente"
    const val CHEGADA_DESTINO = "chegada_destino"
    const val SAIDA_RESIDENCIA = "saida_residencia"
}

/**
 * A PESSOA (nome, endereço, coordenadas, última atualização) agora é REAL: vem dos vínculos e da localização que o
 * backend guarda (veja GuardianTracking) e é passada em [build].
 * O restante (status do dispositivo, alertas, notificações) continua DEMONSTRATIVO e não representa ninguém real,
 * pois o backend ainda não coleta esses dados. FUTURO: trocar por dados de API mantendo estas classes.
 */
object GuardianDemoData {
    fun build(responsavelNome: String?, pessoa: PessoaAcompanhada): GuardianData {
        return GuardianData(
            responsavel = Responsavel(responsavelNome.orEmpty().trim()),
            pessoa = pessoa,
            dispositivo = Dispositivo(status = "Conectado", bateria = 82, localizacaoAtivada = true, iaAtivada = true),
            alertas = listOf(
                Alerta(1, TipoAlerta.ROTA_DIFERENTE, "Rota diferente", "Percurso diferente do habitual.", "há 15 minutos"),
                Alerta(2, TipoAlerta.CHEGADA_DESTINO, "Chegou no destino", "Chegada ao destino informado.", "há 10 minutos"),
                Alerta(3, TipoAlerta.SAIDA_RESIDENCIA, "Saiu de casa", "Saída da residência.", "há 23 min"),
            ),
            notificacoes = listOf(
                Notificacao(1, "${pessoa.nome} está na rota errada"),
                Notificacao(2, "${pessoa.nome} chegou em casa"),
                Notificacao(3, "${pessoa.nome} está indo pra escola"),
            ),
        )
    }
}
