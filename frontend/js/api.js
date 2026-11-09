// ==============================================================================
// COMUNICAÇÃO COM A API HOMELIVE (Spring Boot)
// O login (POST /auth/login) troca e-mail + senha por um TOKEN. Todo request seguinte leva
// "Authorization: Bearer <token>". A senha nunca fica guardada no navegador.
// Quem está agindo vem do login; o front nunca manda solicitanteId/sindicoId.
// ==============================================================================
// Quando o front é servido pela própria API (celular, app instalado), '/api' basta: mesma origem.
// Aberto de arquivo (file://) ou de outro servidor local (ex.: Live Server), aponta para a API em localhost:8080.
const CONFIG = {
    URL_BACKEND: (location.protocol === 'file:' || (location.port && location.port !== '8080'))
        ? 'http://localhost:8080/api'
        : '/api'
};

// --- Sessão (token + dados do usuário logado) ---------------------------------
// "Continuar conectado" guarda no localStorage (token de 30 dias, sobrevive ao fechar o app);
// sem marcar, usa o sessionStorage (some ao fechar a aba).
const Sessao = {
    CHAVE: 'homelive_sessao',
    _storage() {
        return localStorage.getItem(this.CHAVE) ? localStorage : sessionStorage;
    },
    obter() {
        let dados = null;
        try { dados = JSON.parse(this._storage().getItem(this.CHAVE)); } catch { /* sessão corrompida */ }
        // Sessão de versões antigas (sem token) ou com token vencido: não vale mais.
        if (!dados || !dados.token || (dados.expiraEm && Date.parse(dados.expiraEm) <= Date.now())) {
            if (dados) this.limpar();
            return null;
        }
        return dados;
    },
    salvar(dados, lembrar) {
        this.limpar();
        (lembrar ? localStorage : sessionStorage).setItem(this.CHAVE, JSON.stringify(dados));
    },
    limpar() {
        localStorage.removeItem(this.CHAVE);
        sessionStorage.removeItem(this.CHAVE);
    }
};

class ErroApi extends Error {
    constructor(status, mensagem) {
        super(mensagem);
        this.status = status;
    }
}

// --- Cliente HTTP --------------------------------------------------------------
async function http(metodo, rota, corpo, semSessao) {
    const sessao = semSessao ? null : Sessao.obter();
    const headers = {};
    if (sessao) headers['Authorization'] = 'Bearer ' + sessao.token;
    if (corpo !== undefined) headers['Content-Type'] = 'application/json';

    let res;
    try {
        res = await fetch(CONFIG.URL_BACKEND + rota, {
            method: metodo,
            headers,
            body: corpo !== undefined ? JSON.stringify(corpo) : undefined
        });
    } catch {
        throw new ErroApi(0, 'Não consegui falar com a API. Ela está rodando em ' + CONFIG.URL_BACKEND + '?');
    }

    if (res.status === 401 && !semSessao) {
        // sessão inválida/expirada: volta pro login
        Sessao.limpar();
        if (!location.pathname.endsWith('login.html')) location.href = 'login.html';
    }
    if (res.status === 204) return null;

    let dados = null;
    try { dados = await res.json(); } catch { /* corpo vazio */ }
    if (!res.ok) throw new ErroApi(res.status, mensagemDeErro(dados, res.status));
    return dados;
}

// O GlobalExceptionHandler devolve JSON; pega a mensagem mais útil que tiver.
function mensagemDeErro(dados, status) {
    if (dados) {
        if (dados.campos && typeof dados.campos === 'object') {
            const v = Object.values(dados.campos);
            if (v.length) return v.join(' · ');
        }
        if (dados.message) return dados.message;
    }
    if (status === 403) return 'Você não tem permissão para isso.';
    return 'Erro ' + status + ' ao falar com a API.';
}

// App instalável: registra o service worker (só funciona em localhost ou HTTPS; fora disso o site segue normal).
if ('serviceWorker' in navigator && location.protocol.startsWith('http')) {
    window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => { /* sem SW, sem problema */ }));
}

const get = (rota) => http('GET', rota);
const post = (rota, corpo) => http('POST', rota, corpo === undefined ? {} : corpo);
const patch = (rota, corpo) => http('PATCH', rota, corpo);
const del = (rota) => http('DELETE', rota);

// ==============================================================================
// SERVIÇOS (um por módulo da API)
// ==============================================================================
const AuthService = {
    // Devolve { token, expiraEm, papel, usuario }. "lembrar" pede o token de 30 dias.
    async login(email, senha, lembrar) {
        email = email.trim().toLowerCase(); // a API compara o e-mail diferenciando maiúsculas
        return http('POST', '/auth/login', { email, senha, lembrar: !!lembrar }, true);
    },

    // A própria pessoa troca a senha (informando a atual). Erro de senha atual vem como 400, sem deslogar.
    alterarSenha: (senhaAtual, novaSenha) => patch('/auth/senha', { senhaAtual, novaSenha }),

    // Cria a conta e já devolve { token, expiraEm, papel, usuario } (a pessoa sai logada).
    // Sem residência ainda: envie nomeCasa (vira síndico). Com residência: codigoConvite + numeroQuarto (vira morador).
    cadastrar: (dados) => http('POST', '/auth/cadastro', dados, true)
};

const CasaService = {
    // Pública: { casaCriada, nomeCasa }. A tela de cadastro usa para escolher entre "criar" e "entrar".
    estado: () => http('GET', '/casa/estado', undefined, true),
    obter: () => get('/casa'),                        // { nome, dataCriacao, codigoConvite (só síndico) }
    renovarConvite: () => post('/casa/convite/renovar')
};

const DashboardService = { obter: () => get('/dashboard') };

const MoradorService = {
    listar: () => get('/moradores'),
    obter: (id) => get(`/moradores/${id}`),
    ranking: () => get('/moradores/ranking'),
    cadastrar: (m) => post('/moradores', m),
    redefinirSenha: (id) => post(`/moradores/${id}/redefinir-senha`),   // síndico: { senhaTemporaria }
    desativar: (id) => del(`/moradores/${id}`)
};

const SindicoService = { listar: () => get('/sindicos') };

const LeituraService = {
    listar: (competencia) => get('/leituras-consumo' + (competencia ? `?competencia=${competencia}` : '')),
    doMorador: (id) => get(`/leituras-consumo/morador/${id}`),
    registrar: (l) => post('/leituras-consumo', l)
};

const DespesaService = {
    listar: () => get('/despesas'),
    rateiosDoMorador: (id) => get(`/despesas/morador/${id}/rateios`),
    lancar: (d) => post('/despesas', d),
    avaliar: (id, aprovado, motivoRejeicao) => post(`/despesas/${id}/avaliar`, { aprovado, motivoRejeicao }),
    quitar: (itemId) => patch(`/despesas/rateio/${itemId}/quitar`)
};

const TarefaService = {
    listar: () => get('/tarefas'),
    criar: (t) => post('/tarefas', t),
    concluir: (id) => patch(`/tarefas/${id}/concluir`),
    processarAtrasos: () => post('/tarefas/processar-atrasos')
};

const EscalaService = {
    listar: () => get('/tarefas-recorrentes'),
    criar: (e) => post('/tarefas-recorrentes', e),
    ativar: (id) => patch(`/tarefas-recorrentes/${id}/ativar`),
    desativar: (id) => patch(`/tarefas-recorrentes/${id}/desativar`),
    gerar: () => post('/tarefas-recorrentes/gerar')
};

const EspacoService = {
    listar: () => get('/espacos'),                       // todos, ativos e desativados
    criar: (e) => post('/espacos', e),                   // síndico
    alternarStatus: (id) => patch(`/espacos/${id}/status`) // síndico: desativa ou reativa
};

const AgendamentoService = {
    listar: () => get('/agendamentos'),
    criar: (a) => post('/agendamentos', a),
    cancelar: (id) => patch(`/agendamentos/${id}/cancelar`),
    naoCompareceu: (id) => patch(`/agendamentos/${id}/nao-compareceu`)
};

const RegraService = {
    listar: () => get('/regras-penalidade'),
    criar: (r) => post('/regras-penalidade', r),
    ativar: (id) => patch(`/regras-penalidade/${id}/ativar`),
    desativar: (id) => patch(`/regras-penalidade/${id}/desativar`)
};

const PenalidadeService = {
    listar: () => get('/penalidades'),
    doMorador: (id) => get(`/penalidades/morador/${id}`),
    aplicar: (p) => post('/penalidades', p)
};
