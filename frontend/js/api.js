// ==============================================================================
// COMUNICAÇÃO COM A API HOMELIVE (Spring Boot)
// A API usa HTTP Basic: todo request leva "Authorization: Basic base64(email:senha)".
// Quem está agindo vem do login; o front nunca manda solicitanteId/sindicoId.
// ==============================================================================
const CONFIG = {
    URL_BACKEND: 'http://localhost:8080/api'
};

// --- Sessão (credenciais + dados do usuário logado) ---------------------------
// sessionStorage some ao fechar a aba; "lembrar de mim" usa localStorage.
const Sessao = {
    CHAVE: 'homelive_sessao',
    _storage() {
        return localStorage.getItem(this.CHAVE) ? localStorage : sessionStorage;
    },
    obter() {
        try { return JSON.parse(this._storage().getItem(this.CHAVE)); } catch { return null; }
    },
    salvar(dados, lembrar) {
        this.limpar();
        (lembrar ? localStorage : sessionStorage).setItem(this.CHAVE, JSON.stringify(dados));
    },
    limpar() {
        localStorage.removeItem(this.CHAVE);
        sessionStorage.removeItem(this.CHAVE);
    },
    // btoa só aceita Latin-1; o encode evita quebrar com senha acentuada.
    cabecalho(email, senha) {
        const bytes = new TextEncoder().encode(`${email}:${senha}`);
        return 'Basic ' + btoa(String.fromCharCode(...bytes));
    }
};

class ErroApi extends Error {
    constructor(status, mensagem) {
        super(mensagem);
        this.status = status;
    }
}

// --- Cliente HTTP --------------------------------------------------------------
async function http(metodo, rota, corpo, auth) {
    const sessao = Sessao.obter();
    const headers = {};
    const credencial = auth || (sessao && sessao.auth);
    if (credencial) headers['Authorization'] = credencial;
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

    if (res.status === 401 && !auth) {
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

const get = (rota) => http('GET', rota);
const post = (rota, corpo) => http('POST', rota, corpo === undefined ? {} : corpo);
const patch = (rota, corpo) => http('PATCH', rota, corpo);
const del = (rota) => http('DELETE', rota);

// ==============================================================================
// SERVIÇOS (um por módulo da API)
// ==============================================================================
const AuthService = {
    // Não existe /auth/login: testa as credenciais numa rota protegida e descobre
    // o perfil procurando o e-mail entre síndicos e moradores.
    async login(email, senha) {
        email = email.trim().toLowerCase(); // a API compara o e-mail diferenciando maiúsculas
        const auth = Sessao.cabecalho(email, senha);
        let sindicos;
        try {
            sindicos = await http('GET', '/sindicos', undefined, auth);
        } catch (e) {
            if (e.status === 401) throw new ErroApi(401, 'E-mail ou senha incorretos.');
            throw e;
        }
        const sindico = sindicos.find(s => s.email.toLowerCase() === email);
        if (sindico) return { auth, papel: 'SINDICO', usuario: sindico };

        const moradores = await http('GET', '/moradores', undefined, auth);
        const morador = moradores.find(m => m.email.toLowerCase() === email);
        if (!morador) throw new ErroApi(401, 'Não encontrei seu perfil no sistema.');
        return { auth, papel: 'MORADOR', usuario: morador };
    }
};

const DashboardService = { obter: () => get('/dashboard') };

const MoradorService = {
    listar: () => get('/moradores'),
    obter: (id) => get(`/moradores/${id}`),
    ranking: () => get('/moradores/ranking'),
    cadastrar: (m) => post('/moradores', m),
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

const EspacoService = { listar: () => get('/espacos') };

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
