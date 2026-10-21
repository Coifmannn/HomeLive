// ==============================================================================
// INTERFACE (VIEW). A comunicação com a API está em api.js.
// ==============================================================================

// --- Utilidades ----------------------------------------------------------------
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const brl = (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const fmtData = (iso) => iso ? iso.slice(0, 10).split('-').reverse().join('/') : '—';
const fmtHora = (iso) => iso.slice(11, 16);
const fmtCompetencia = (c) => c ? c.split('-').reverse().join('/') : '';
const valor = (id) => document.getElementById(id).value;
const numOuNulo = (id) => { const v = valor(id); return v === '' ? null : Number(v); };
const textoOuNulo = (id) => valor(id).trim() || null;

function hojeIso() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// Foto de perfil ainda não existe na API: gera um avatar com as iniciais.
function avatar(nome) {
    const partes = String(nome || '?').trim().split(/\s+/);
    const iniciais = (partes[0][0] + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
    let hash = 0;
    for (const ch of String(nome)) hash = (hash * 31 + ch.charCodeAt(0)) % 360;
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" fill="hsl(${hash},55%,38%)"/>` +
        `<text x="50%" y="54%" font-family="sans-serif" font-size="26" font-weight="600" fill="#fff" text-anchor="middle" dominant-baseline="middle">${iniciais}</text></svg>`;
    return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg);
}

const ROTULOS = {
    infracao: { TAREFA_ATRASADA: 'Tarefa atrasada', CANCELAMENTO_TARDIO: 'Cancelamento tardio', NAO_COMPARECIMENTO: 'Não comparecimento', MANUAL: 'Manual' },
    periodicidade: { DIARIA: 'Diária', SEMANAL: 'Semanal', QUINZENAL: 'Quinzenal', MENSAL: 'Mensal' },
    rodizio: { SEQUENCIAL: 'Sequencial', MENOR_PONTUACAO: 'Menor pontuação', MENOR_CARGA: 'Menor carga' },
    rateio: { IGUALITARIO: 'Igualitário', POR_ESTADIA: 'Por estadia', POR_CONSUMO: 'Por consumo' },
    tarefa: { PENDENTE: 'Pendente', CONCLUIDA: 'Concluída', ATRASADA: 'Atrasada', CANCELADA: 'Cancelada' },
    agendamento: { CONFIRMADO: 'Confirmada', CANCELADO: 'Cancelada', NAO_COMPARECEU: 'Não compareceu' }
};

// --- Estado da tela --------------------------------------------------------------
const S = {
    sessao: null, eu: null, ehSindico: false,
    moradores: [], sindicos: [], despesas: [], tarefas: [], escalas: [],
    espacos: [], agendamentos: [], regras: [], penalidades: [], leituras: [], dash: null
};

document.addEventListener('DOMContentLoaded', () => {
    const naAplicacao = !!document.getElementById('visao-geral');

    if (naAplicacao) {
        S.sessao = Sessao.obter();
        if (!S.sessao) { location.href = 'login.html'; return; }
        S.eu = S.sessao.usuario;
        S.ehSindico = S.sessao.papel === 'SINDICO';
        document.body.classList.toggle('modo-sindico', S.ehSindico);
        iniciarAplicacao();
    }
    iniciarFormulariosDeAcesso();
});

// ==============================================================================
// APLICAÇÃO
// ==============================================================================
function iniciarAplicacao() {
    // Navegação entre telas
    document.querySelectorAll('.links-navegacao li').forEach(link => {
        link.addEventListener('click', () => {
            document.querySelectorAll('.links-navegacao li').forEach(l => l.classList.remove('ativo'));
            document.querySelectorAll('.tela').forEach(t => t.classList.remove('tela-ativa'));
            link.classList.add('ativo');
            document.getElementById(link.getAttribute('data-alvo')).classList.add('tela-ativa');
            document.getElementById('titulo-pagina').textContent = link.querySelector('span').textContent;
        });
    });

    // Menu mobile
    const menu = document.querySelector('.menu-lateral');
    const overlay = document.getElementById('overlay-menu');
    const fechar = () => { menu.classList.remove('aberto'); overlay.classList.remove('ativo'); };
    document.getElementById('toggle-menu').addEventListener('click', () => { menu.classList.toggle('aberto'); overlay.classList.toggle('ativo'); });
    overlay.addEventListener('click', fechar);
    document.querySelectorAll('.links-navegacao li').forEach(l => l.addEventListener('click', fechar));

    document.getElementById('botao-sair').addEventListener('click', () => Sessao.limpar());

    // Cabeçalho
    document.getElementById('exibicao-data').textContent =
        new Date().toLocaleDateString('pt-BR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    document.getElementById('nome-usuario').textContent = S.eu.nome.split(' ').find(p => !p.endsWith('.')) || S.eu.nome;
    document.getElementById('avatar-usuario').src = avatar(S.eu.nome);
    document.getElementById('exibicao-papel').textContent = S.ehSindico ? 'Líder (Síndico)' : 'Morador';
    if (S.ehSindico) {
        document.getElementById('titulo-proximas-escalas').textContent = 'Tarefas em Aberto da Casa';
        document.getElementById('th-rateio').textContent = 'Parcelas';
        document.getElementById('titulo-penalidades').textContent = 'Histórico de Penalidades';
    }

    iniciarEventos();
    iniciarFormulariosDaAplicacao();
    renderTudo();
}

async function carregarDados() {
    const ehS = S.ehSindico;
    const [moradores, sindicos, despesas, tarefas, escalas, espacos, agendamentos, regras, penalidades, dash, leituras] =
        await Promise.all([
            MoradorService.listar(), SindicoService.listar(), DespesaService.listar(), TarefaService.listar(),
            EscalaService.listar(), EspacoService.listar(), AgendamentoService.listar(), RegraService.listar(),
            ehS ? PenalidadeService.listar() : PenalidadeService.doMorador(S.eu.id),
            DashboardService.obter(),
            ehS ? Promise.resolve([]) : LeituraService.doMorador(S.eu.id)
        ]);
    Object.assign(S, { moradores, sindicos, despesas, tarefas, escalas, espacos, agendamentos, regras, penalidades, dash, leituras });
}

async function renderTudo() {
    try {
        await carregarDados();
    } catch (e) {
        mostrarToast(e.message, 'erro');
        return;
    }
    renderCabecalho();
    renderVisaoGeral();
    renderDespesas();
    renderTarefas();
    renderEscalas();
    renderEspacos();
    renderAgendamentos();
    renderMoradores();
    renderRegras();
    renderPenalidades();
    popularSelects();
}

// --- Dados derivados ---------------------------------------------------------------
const meuItem = (d) => (d.itensRateio || []).find(i => i.moradorId === S.eu.id);
const tarefaAberta = (t) => t.status === 'PENDENTE' || t.status === 'ATRASADA';
const minhasTarefasAbertas = () => S.tarefas.filter(t => tarefaAberta(t) && (S.ehSindico || t.responsavelId === S.eu.id));

function renderCabecalho() {
    const badge = document.getElementById('distintivo-pendencias');
    if (S.ehSindico) {
        document.getElementById('pontos-usuario').textContent = 'Síndico';
        badge.textContent = S.despesas.filter(d => d.status === 'PENDENTE_APROVACAO').length;
    } else {
        const eu = S.moradores.find(m => m.id === S.eu.id);
        document.getElementById('pontos-usuario').textContent = `${eu ? eu.pontosConvivencia : 0} pts`;
        badge.textContent = minhasTarefasAbertas().length;
    }
}

// --- Visão geral -------------------------------------------------------------------
function definirEstatistica(n, titulo, valorTxt, sub) {
    document.getElementById(`est${n}-titulo`).textContent = titulo;
    document.getElementById(`est${n}-valor`).textContent = valorTxt;
    document.getElementById(`est${n}-sub`).textContent = sub;
}

function renderVisaoGeral() {
    const d = S.dash;
    if (S.ehSindico) {
        const pendentes = S.despesas.filter(x => x.status === 'PENDENTE_APROVACAO').length;
        definirEstatistica(1, 'Despesas do Mês', brl(d.totalDespesasMes), `${pendentes} aguardando sua aprovação`);
        definirEstatistica(2, 'Tarefas Pendentes', String(d.totalTarefasPendentes), 'em aberto na casa');
        definirEstatistica(3, 'Reservas Hoje', String(d.totalAgendamentosHoje), `${d.totalMoradores} moradores na casa`);
    } else {
        const abertos = S.despesas.map(meuItem).filter(i => i && i.statusPagamento === 'PENDENTE');
        definirEstatistica(1, 'Seu Rateio Pendente', brl(abertos.reduce((s, i) => s + Number(i.valorCalculado), 0)),
            abertos.length ? `${abertos.length} parcela(s) em aberto` : 'Tudo pago!');

        const ultima = [...S.leituras].sort((a, b) => b.competencia.localeCompare(a.competencia))[0];
        definirEstatistica(2, 'Seu Último Consumo', ultima ? `${ultima.kwh} kWh` : '—',
            ultima ? `Leitura de ${fmtCompetencia(ultima.competencia)}` : 'Nenhuma leitura registrada');

        const ranking = d.rankingMoradores;
        const pos = ranking.findIndex(m => m.id === S.eu.id);
        const topo = ranking[0];
        definirEstatistica(3, 'Ranking da Casa', pos >= 0 ? `${pos + 1}º Lugar` : '—',
            pos <= 0 ? 'Você lidera a casa! 🏆' : `Faltam ${topo.pontosConvivencia - ranking[pos].pontosConvivencia} pts para o topo`);
    }

    const lista = minhasTarefasAbertas().sort((a, b) => a.dataPrazo.localeCompare(b.dataPrazo)).slice(0, 4);
    document.getElementById('dash-tarefas').innerHTML = lista.length ? lista.map(t => `
        <li class="item-tarefa">
            <div class="info-tarefa">
                <div class="icone-tarefa"><i class='bx bx-task'></i></div>
                <div>
                    <h4>${esc(t.titulo)}</h4>
                    <p>${S.ehSindico ? esc(t.responsavelNome) + ' · ' : ''}Prazo ${fmtData(t.dataPrazo)}${t.status === 'ATRASADA' ? ' · atrasada' : ''}</p>
                </div>
            </div>
            <div class="acao-tarefa">
                <span class="pontos-verde">+${t.pontosRecompensa ?? 0} pts</span>
                <button class="botao-primario botao-pequeno" data-acao="concluir-tarefa" data-id="${t.id}">Concluir</button>
            </div>
        </li>`).join('') : '<li class="item-tarefa"><p>Nenhuma tarefa em aberto. 🎉</p></li>';

    const reservas = d.proximosAgendamentos;
    document.getElementById('dash-reservas').innerHTML = reservas.length ? reservas.map(a => `
        <div class="cartao-reserva">
            <i class='bx bx-calendar-check'></i>
            <div class="detalhes-r">
                <h4>${esc(a.espacoNome)}</h4>
                <p>${esc(a.moradorNome)} · ${fmtData(a.dataHoraInicio)}, ${fmtHora(a.dataHoraInicio)} - ${fmtHora(a.dataHoraFim)}</p>
            </div>
        </div>`).join('') : '<p style="color:var(--texto-suave);">Nenhuma reserva próxima.</p>';
}

// --- Finanças ----------------------------------------------------------------------
function renderDespesas() {
    const linhas = [...S.despesas].sort((a, b) => b.id - a.id).map(d => {
        const item = meuItem(d);
        const itens = d.itensRateio || [];
        let rateio, status, classe, acao = '';

        if (S.ehSindico) {
            rateio = itens.length ? `${itens.filter(i => i.statusPagamento === 'PAGO').length}/${itens.length} pagas` : '—';
        } else {
            rateio = item ? brl(item.valorCalculado) : '—';
        }

        switch (d.status) {
            case 'PENDENTE_APROVACAO': status = 'Aguardando aprovação'; classe = 'status-pendente'; break;
            case 'APROVADA': status = 'Aprovada'; classe = 'status-pendente'; break;
            case 'REJEITADA': status = 'Rejeitada'; classe = 'status-ocupado'; break;
            default:
                if (item) { status = item.statusPagamento === 'PAGO' ? 'Pago' : 'A pagar'; classe = item.statusPagamento === 'PAGO' ? 'status-pago' : 'status-pendente'; }
                else { status = 'Rateada'; classe = 'status-pago'; }
        }

        if (S.ehSindico && d.status === 'PENDENTE_APROVACAO') {
            acao = `<button class="botao-primario botao-pequeno" data-acao="aprovar-despesa" data-id="${d.id}">Aprovar</button>
                    <button class="botao-perigo botao-pequeno" data-acao="rejeitar-despesa" data-id="${d.id}">Rejeitar</button>`;
        } else if (item && item.statusPagamento === 'PENDENTE') {
            acao = `<button class="botao-contorno botao-pequeno" data-acao="quitar" data-id="${item.id}">Quitar</button>`;
        } else if (d.status === 'REJEITADA' && d.motivoRejeicao) {
            acao = `<small style="color:var(--texto-suave);">${esc(d.motivoRejeicao)}</small>`;
        }

        return `<tr>
            <td>${esc(d.titulo)}<br><small style="color:var(--texto-suave);">${ROTULOS.rateio[d.tipoRateio]}${d.competencia ? ' · ' + fmtCompetencia(d.competencia) : ''}</small></td>
            <td><span class="etiqueta ${d.tipoDespesa === 'FIXA' ? 'etiqueta-fixa' : 'etiqueta-variavel'}">${d.tipoDespesa === 'FIXA' ? 'Fixa' : 'Variável'}</span></td>
            <td>${brl(d.valorTotal)}</td>
            <td>${rateio}</td>
            <td><span class="status ${classe}">${status}</span></td>
            <td>${acao}</td>
        </tr>`;
    });
    document.getElementById('lista-despesas').innerHTML = linhas.join('') ||
        '<tr><td colspan="6" style="padding:20px 15px;color:var(--texto-suave);">Nenhuma despesa lançada.</td></tr>';
}

// --- Tarefas e escalas --------------------------------------------------------------
function renderTarefas() {
    const ordem = [...S.tarefas].sort((a, b) =>
        (tarefaAberta(b) - tarefaAberta(a)) || a.dataPrazo.localeCompare(b.dataPrazo));
    document.getElementById('lista-tarefas').innerHTML = ordem.map(t => {
        const aberta = tarefaAberta(t);
        const podeConcluir = aberta && (S.ehSindico || t.responsavelId === S.eu.id);
        const acao = t.status === 'CONCLUIDA' ? `<span class="texto-verde"><i class='bx bx-check-double'></i> Concluída</span>`
            : t.status === 'CANCELADA' ? `<span style="color:var(--texto-suave);">Cancelada</span>`
                : podeConcluir ? `<button data-acao="concluir-tarefa" data-id="${t.id}" class="botao-primario botao-pequeno">Concluir</button>` : '';
        return `
        <div class="cartao-tarefa painel-vidro" style="${aberta ? '' : 'opacity:0.6;'}">
            <div class="cabecalho-t">
                <span class="etiqueta-t ${t.status === 'ATRASADA' ? 'etiqueta-urgente' : ''}">${t.status === 'ATRASADA' ? 'Atrasada · ' : ''}${fmtData(t.dataPrazo)}</span>
                <span class="pontos-t">+${t.pontosRecompensa ?? 0} pts</span>
            </div>
            <h3>${esc(t.titulo)}</h3>
            <p>${esc(t.descricao || '')}</p>
            <div class="rodape-t">
                <div class="responsavel"><img src="${avatar(t.responsavelNome)}" alt=""><span>${esc(t.responsavelNome)}</span></div>
                ${acao}
            </div>
        </div>`;
    }).join('') || '<p style="color:var(--texto-suave);">Nenhuma tarefa cadastrada.</p>';
}

function renderEscalas() {
    document.getElementById('lista-escalas').innerHTML = S.escalas.map(e => `
        <div class="cartao-tarefa painel-vidro" style="${e.ativa ? '' : 'opacity:0.6;'}">
            <div class="cabecalho-t">
                <span class="etiqueta-t">${ROTULOS.periodicidade[e.periodicidade]}</span>
                <span class="pontos-t">+${e.pontosRecompensa ?? 0} pts</span>
            </div>
            <h3>${esc(e.titulo)}</h3>
            <p>${esc(e.descricao || '')}</p>
            <p style="font-size:0.8rem;color:var(--texto-suave);margin-top:8px;">
                Rodízio: ${ROTULOS.rodizio[e.tipoRodizio]} · Prazo: ${e.prazoEmDias} dia(s)<br>
                Próxima geração: ${fmtData(e.proximaGeracao)}${e.ultimoResponsavelNome ? ' · Último: ' + esc(e.ultimoResponsavelNome) : ''}
            </p>
            <div class="rodape-t">
                <span class="${e.ativa ? 'texto-verde' : ''}">${e.ativa ? 'Ativa' : 'Pausada'}</span>
                <button class="botao-contorno botao-pequeno apenas-admin" data-acao="toggle-escala" data-id="${e.id}" data-ativa="${e.ativa}">${e.ativa ? 'Pausar' : 'Ativar'}</button>
            </div>
        </div>`).join('') || '<p style="color:var(--texto-suave);">Nenhuma escala recorrente.</p>';
}

// --- Reservas ----------------------------------------------------------------------
function imagemDoEspaco(nome) {
    const n = nome.toLowerCase();
    if (n.includes('escrit')) return 'acustico';
    if (n.includes('lavand')) return 'lavanderia';
    if (n.includes('gourmet')) return 'gourmet';
    return '';
}

function renderEspacos() {
    const agora = new Date();
    document.getElementById('lista-espacos').innerHTML = S.espacos.map(e => {
        const emUso = S.agendamentos.some(a => a.espacoId === e.id && a.status === 'CONFIRMADO'
            && new Date(a.dataHoraInicio) <= agora && new Date(a.dataHoraFim) > agora);
        const status = !e.ativo ? ['Desativado', 'status-ocupado'] : emUso ? ['Ocupado agora', 'status-ocupado'] : ['Livre agora', 'status-disponivel'];
        return `
        <div class="cartao-espaco painel-vidro">
            <div class="img-espaco ${imagemDoEspaco(e.nome)}"></div>
            <div class="conteudo-espaco">
                <h3>${esc(e.nome)}</h3>
                <p>${esc(e.descricao || e.regrasUso || '')}</p>
                <div class="meta-espaco">
                    <span><i class='bx bx-group'></i> ${e.capacidadeMaxima ? 'Até ' + e.capacidadeMaxima : 'Sem limite'}</span>
                    <span class="status ${status[1]}">${status[0]}</span>
                </div>
                ${e.ativo
                ? `<button class="botao-primario largura-total" data-acao="reservar-espaco" data-id="${e.id}" data-nome="${esc(e.nome)}">Reservar Espaço</button>`
                : `<button class="botao-contorno largura-total" disabled>Indisponível</button>`}
            </div>
        </div>`;
    }).join('') || '<p style="color:var(--texto-suave);">Nenhum espaço cadastrado.</p>';
}

function renderAgendamentos() {
    const agora = new Date();
    const lista = [...S.agendamentos].sort((a, b) => b.dataHoraInicio.localeCompare(a.dataHoraInicio)).slice(0, 30);
    document.getElementById('lista-agendamentos').innerHTML = lista.map(a => {
        const confirmado = a.status === 'CONFIRMADO';
        const comecou = new Date(a.dataHoraInicio) <= agora;
        const terminou = new Date(a.dataHoraFim) <= agora;
        const classe = confirmado ? 'status-disponivel' : a.status === 'CANCELADO' ? 'status-pendente' : 'status-ocupado';
        let acao = '';
        if (confirmado && !terminou && (S.ehSindico || a.moradorId === S.eu.id)) {
            acao += `<button class="botao-perigo botao-pequeno" data-acao="cancelar-reserva" data-id="${a.id}">Cancelar</button> `;
        }
        if (S.ehSindico && confirmado && comecou) {
            acao += `<button class="botao-contorno botao-pequeno" data-acao="nao-compareceu" data-id="${a.id}">Não compareceu</button>`;
        }
        return `<tr>
            <td>${esc(a.espacoNome)}</td>
            <td>${esc(a.moradorNome)}</td>
            <td>${fmtData(a.dataHoraInicio)} ${fmtHora(a.dataHoraInicio)} - ${fmtHora(a.dataHoraFim)}</td>
            <td><span class="status ${classe}">${ROTULOS.agendamento[a.status]}</span></td>
            <td>${acao}</td>
        </tr>`;
    }).join('') || '<tr><td colspan="5" style="padding:20px 15px;color:var(--texto-suave);">Nenhuma reserva.</td></tr>';
}

// --- Moradores ---------------------------------------------------------------------
function renderMoradores() {
    const cartao = (nome, papel, xp, ativo, extra, botao) => `
        <div class="cartao-morador painel-vidro" style="${ativo ? '' : 'opacity:0.5;'}">
            <div class="cabecalho-morador">
                <div class="container-foto"><img src="${avatar(nome)}" alt="" style="object-fit: cover;"></div>
                <div><h3>${esc(nome)}</h3><span class="etiqueta ${papel === 'SINDICO' ? 'etiqueta-lider' : 'etiqueta-papel'}">${papel === 'SINDICO' ? 'Líder (Síndico)' : 'Morador'}</span></div>
            </div>
            <div class="estatisticas-morador">
                <div class="estat"><span>${papel === 'SINDICO' ? 'Pontos' : 'Pontos'}</span><strong>${xp}</strong></div>
                <div class="estat"><span>${extra[0]}</span><strong>${esc(extra[1])}</strong></div>
                <div class="estat"><span>Status</span><strong class="${ativo ? 'texto-verde' : ''}">${ativo ? 'Ativo' : 'Inativo'}</strong></div>
            </div>
            ${botao}
        </div>`;

    const sindicos = S.sindicos.map(s => cartao(s.nome, 'SINDICO', '—', s.ativo, ['Depto.', s.departamento || '—'], ''));
    const moradores = S.moradores.map(m => cartao(m.nome, 'MORADOR', `${m.pontosConvivencia} pts`, m.ativo, ['Quarto', m.numeroQuarto],
        S.ehSindico && m.ativo ? `<button data-acao="desativar-morador" data-id="${m.id}" class="botao-perigo botao-pequeno largura-total mt-3">Desativar Morador</button>` : ''));
    document.getElementById('lista-moradores').innerHTML = sindicos.concat(moradores).join('');
}

// --- Regras e penalidades ----------------------------------------------------------
function descreverRegra(r) {
    let txt = r.modoCalculo === 'FIXA'
        ? `${r.pontosBase} pts fixos`
        : `${r.pontosBase} pts + ${r.incrementoPorReincidencia} por reincidência (últimos ${r.janelaReincidenciaDias} dias)${r.tetoPontos ? ', teto ' + r.tetoPontos : ''}`;
    if (r.limiteHorasAntecedencia) txt += `. Vale se cancelar com menos de ${r.limiteHorasAntecedencia}h de antecedência`;
    return txt;
}

function renderRegras() {
    document.getElementById('lista-regras').innerHTML = S.regras.map(r => `
        <div class="cartao-votacao painel-vidro">
            <div class="status-votacao ${r.ativa ? 'tag-ativa' : 'tag-encerrada'}">${r.ativa ? 'Ativa' : 'Inativa'}</div>
            <h3>${esc(r.nome)}</h3>
            <p><strong>${ROTULOS.infracao[r.tipoInfracao]}</strong> · ${descreverRegra(r)}</p>
            <div class="acoes-votar apenas-admin">
                <button class="${r.ativa ? 'botao-perigo' : 'botao-contorno'}" data-acao="toggle-regra" data-id="${r.id}" data-ativa="${r.ativa}">${r.ativa ? 'Desativar' : 'Ativar'}</button>
            </div>
        </div>`).join('') || '<p style="color:var(--texto-suave);">Nenhuma regra cadastrada.</p>';
}

function renderPenalidades() {
    const lista = [...S.penalidades].sort((a, b) => b.dataAplicacao.localeCompare(a.dataAplicacao));
    document.getElementById('lista-penalidades').innerHTML = lista.map(p => `<tr>
        <td>${fmtData(p.dataAplicacao)}</td>
        <td>${esc(p.moradorNome)}</td>
        <td>${ROTULOS.infracao[p.tipoInfracao]}</td>
        <td class="texto-vermelho" style="color:var(--destaque-vermelho);">-${p.pontos}</td>
        <td>${esc(p.motivo)}</td>
        <td>${esc(p.aplicadaPorNome || 'Sistema Automático')}</td>
    </tr>`).join('') || '<tr><td colspan="6" style="padding:20px 15px;color:var(--texto-suave);">Nenhuma penalidade registrada. 👏</td></tr>';
}

// --- Selects de morador (modais) ---------------------------------------------------
function popularSelects() {
    const ativos = S.moradores.filter(m => m.ativo);
    const opcoes = ativos.map(m => `<option value="${m.id}">${esc(m.nome)} (${esc(m.numeroQuarto)})</option>`).join('');
    ['reserva-morador', 'leitura-morador', 'nova-tarefa-responsavel', 'penalidade-morador'].forEach(id => {
        const sel = document.getElementById(id);
        const atual = sel.value;
        sel.innerHTML = opcoes;
        if (atual) sel.value = atual;
    });
}

// ==============================================================================
// AÇÕES (botões das listas, via delegação de eventos)
// ==============================================================================
async function executar(promessa, mensagemOk, tipo = 'sucesso') {
    try {
        await promessa;
        if (mensagemOk) mostrarToast(mensagemOk, tipo);
        await renderTudo();
        return true;
    } catch (e) {
        mostrarToast(e.message, 'erro');
        return false;
    }
}

function iniciarEventos() {
    document.addEventListener('click', (ev) => {
        const el = ev.target.closest('[data-acao]');
        if (!el) return;
        const id = Number(el.dataset.id);
        const ativa = el.dataset.ativa === 'true';

        switch (el.dataset.acao) {
            case 'concluir-tarefa':
                executar(TarefaService.concluir(id), 'Tarefa concluída! Pontos creditados 🏆'); break;
            case 'quitar':
                executar(DespesaService.quitar(id), 'Parcela quitada ✅'); break;
            case 'aprovar-despesa':
                executar(DespesaService.avaliar(id, true, null), 'Despesa aprovada e rateada!'); break;
            case 'rejeitar-despesa': {
                const motivo = prompt('Motivo da rejeição:');
                if (motivo === null) return;
                executar(DespesaService.avaliar(id, false, motivo || 'Sem motivo informado'), 'Despesa rejeitada.', 'aviso');
                break;
            }
            case 'desativar-morador':
                if (confirm('Desativar este morador?')) executar(MoradorService.desativar(id), 'Morador desativado.', 'aviso');
                break;
            case 'toggle-escala':
                executar(ativa ? EscalaService.desativar(id) : EscalaService.ativar(id), ativa ? 'Escala pausada.' : 'Escala ativada.', 'info'); break;
            case 'toggle-regra':
                executar(ativa ? RegraService.desativar(id) : RegraService.ativar(id), ativa ? 'Regra desativada.' : 'Regra ativada.', 'info'); break;
            case 'cancelar-reserva':
                if (confirm('Cancelar esta reserva?')) executar(AgendamentoService.cancelar(id), 'Reserva cancelada.', 'aviso');
                break;
            case 'nao-compareceu':
                if (confirm('Registrar que o morador não compareceu? Isso pode gerar penalidade.')) executar(AgendamentoService.naoCompareceu(id), 'Não comparecimento registrado.', 'aviso');
                break;
            case 'processar-atrasos':
                executar(TarefaService.processarAtrasos(), 'Atrasos processados.', 'info'); break;
            case 'gerar-escalas':
                executar(EscalaService.gerar(), 'Tarefas devidas geradas pelo rodízio.', 'info'); break;
            case 'reservar-espaco':
                document.getElementById('reserva-espaco-id').value = id;
                document.getElementById('reserva-data').min = hojeIso();
                abrirModal('modalReserva', el.dataset.nome);
                break;
        }
    });
}

// ==============================================================================
// FORMULÁRIOS (modais)
// ==============================================================================
function aoEnviar(idForm, modal, mensagemOk, montar) {
    const form = document.getElementById(idForm);
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const ok = await executar(montar(), mensagemOk);
        if (ok) { fecharModal(modal); form.reset(); }
    });
}

function iniciarFormulariosDaAplicacao() {
    aoEnviar('formulario-adicionar-morador', 'modalAdicionarMorador', 'Morador cadastrado!', () =>
        MoradorService.cadastrar({
            nome: valor('novo-morador-nome'), email: valor('novo-morador-email'), senha: valor('novo-morador-senha'),
            numeroQuarto: valor('novo-morador-quarto'), telefone: textoOuNulo('novo-morador-telefone')
        }));

    aoEnviar('formulario-nova-despesa', 'modalNovaDespesa', 'Despesa enviada para aprovação!', () =>
        DespesaService.lancar({
            titulo: valor('nova-despesa-titulo'), descricao: textoOuNulo('nova-despesa-desc'),
            valorTotal: Number(valor('nova-despesa-valor')), dataVencimento: valor('nova-despesa-vencimento'),
            categoria: valor('nova-despesa-categoria'), tipoDespesa: valor('nova-despesa-tipo'),
            tipoRateio: valor('nova-despesa-rateio'), competencia: textoOuNulo('nova-despesa-competencia')
        }));

    aoEnviar('formulario-leitura', 'modalLeitura', 'Leitura registrada!', () =>
        LeituraService.registrar({
            kwh: Number(valor('leitura-kwh')), competencia: textoOuNulo('leitura-competencia'),
            moradorId: S.ehSindico ? Number(valor('leitura-morador')) : null
        }));

    aoEnviar('formulario-nova-tarefa', 'modalNovaTarefa', 'Tarefa criada!', () =>
        TarefaService.criar({
            titulo: valor('nova-tarefa-titulo'), descricao: textoOuNulo('nova-tarefa-desc'),
            dataPrazo: valor('nova-tarefa-prazo'), responsavelId: Number(valor('nova-tarefa-responsavel')),
            pontosRecompensa: numOuNulo('nova-tarefa-xp'), pontosPenalidade: numOuNulo('nova-tarefa-penalidade')
        }));

    aoEnviar('formulario-nova-escala', 'modalNovaEscala', 'Escala criada!', () =>
        EscalaService.criar({
            titulo: valor('escala-titulo'), descricao: textoOuNulo('escala-desc'),
            periodicidade: valor('escala-periodicidade'), prazoEmDias: Number(valor('escala-prazo')),
            tipoRodizio: valor('escala-rodizio'),
            pontosRecompensa: numOuNulo('escala-xp'), pontosPenalidade: numOuNulo('escala-penalidade'),
            primeiraGeracao: textoOuNulo('escala-primeira')
        }));

    aoEnviar('formulario-penalidade', 'modalPenalidade', 'Penalidade aplicada.', () =>
        PenalidadeService.aplicar({
            moradorId: Number(valor('penalidade-morador')), pontos: Number(valor('penalidade-pontos')), motivo: valor('penalidade-motivo')
        }));

    aoEnviar('formulario-reserva', 'modalReserva', 'Reserva confirmada! 📅', () => {
        const dia = valor('reserva-data');
        return AgendamentoService.criar({
            espacoId: Number(valor('reserva-espaco-id')),
            moradorId: S.ehSindico ? Number(valor('reserva-morador')) : null,
            dataHoraInicio: `${dia}T${valor('reserva-inicio')}:00`,
            dataHoraFim: `${dia}T${valor('reserva-fim')}:00`,
            motivoReserva: textoOuNulo('reserva-motivo')
        });
    });

    // Regra de penalidade: só mostra os campos que fazem sentido para o tipo escolhido
    const infracao = document.getElementById('regra-infracao');
    const modo = document.getElementById('regra-modo');
    const ajustarCamposDaRegra = () => {
        document.getElementById('regra-campo-horas').style.display = infracao.value === 'CANCELAMENTO_TARDIO' ? '' : 'none';
        document.getElementById('regra-campos-progressiva').style.display = modo.value === 'PROGRESSIVA' ? '' : 'none';
    };
    infracao.addEventListener('change', ajustarCamposDaRegra);
    modo.addEventListener('change', ajustarCamposDaRegra);
    ajustarCamposDaRegra();

    aoEnviar('formulario-nova-regra', 'modalNovaRegra', 'Regra criada!', () => {
        const progressiva = modo.value === 'PROGRESSIVA';
        const tardio = infracao.value === 'CANCELAMENTO_TARDIO';
        return RegraService.criar({
            nome: valor('regra-nome'), tipoInfracao: infracao.value, modoCalculo: modo.value,
            pontosBase: Number(valor('regra-base')),
            incrementoPorReincidencia: progressiva ? numOuNulo('regra-incremento') : null,
            tetoPontos: progressiva ? numOuNulo('regra-teto') : null,
            janelaReincidenciaDias: progressiva ? numOuNulo('regra-janela') : null,
            limiteHorasAntecedencia: tardio ? numOuNulo('regra-horas') : null
        });
    });
}

// ==============================================================================
// LOGIN / CADASTRO / RECUPERAÇÃO
// ==============================================================================
function iniciarFormulariosDeAcesso() {
    const formLogin = document.getElementById('formulario-login');
    if (formLogin) {
        formLogin.addEventListener('submit', async (e) => {
            e.preventDefault();
            const botao = formLogin.querySelector('button[type=submit]');
            botao.disabled = true;
            try {
                const email = valor('credencial').trim();
                const r = await AuthService.login(email, valor('senha'));
                Sessao.salvar({ auth: r.auth, papel: r.papel, usuario: r.usuario }, document.getElementById('lembrar').checked);
                mostrarToast('Bem-vindo de volta! Redirecionando...', 'sucesso');
                setTimeout(() => { window.location.href = 'index.html'; }, 600);
            } catch (err) {
                mostrarToast(err.message, 'erro');
                botao.disabled = false;
            }
        });
    }

    // A API ainda não tem cadastro público nem recuperação de senha.
    const formCadastro = document.getElementById('formulario-cadastro');
    if (formCadastro) {
        formCadastro.addEventListener('submit', (e) => {
            e.preventDefault();
            mostrarToast('Por enquanto o cadastro é feito pelo síndico, dentro do sistema.', 'info');
        });
    }
    const formRecup = document.getElementById('formulario-recuperacao');
    if (formRecup) {
        formRecup.addEventListener('submit', (e) => {
            e.preventDefault();
            mostrarToast('Recuperação de senha ainda não está disponível. Fale com o síndico.', 'info');
        });
    }
}

// ==============================================================================
// MODAIS E TOASTS
// ==============================================================================
window.abrirModal = function (id, nomeEspaco) {
    document.getElementById(id)?.classList.add('ativa');
    if (nomeEspaco) {
        const titulo = document.getElementById('nome-espaco-modal');
        if (titulo) titulo.textContent = `Reservar: ${nomeEspaco}`;
    }
};
window.fecharModal = function (id) { document.getElementById(id)?.classList.remove('ativa'); };
window.navegarPara = function (alvo) { document.querySelector(`.links-navegacao li[data-alvo="${alvo}"]`)?.click(); };

window.mostrarToast = function (mensagem, tipo = 'sucesso') {
    const container = document.getElementById('container-toasts');
    if (!container) return;
    const icones = { sucesso: 'bx-check-circle', erro: 'bx-x-circle', info: 'bx-info-circle', aviso: 'bx-error' };
    const toast = document.createElement('div');
    toast.className = `toast toast-${tipo}`;
    toast.innerHTML = `<i class='bx ${icones[tipo] || 'bx-info-circle'}'></i><span>${esc(mensagem)}</span>`;
    container.appendChild(toast);
    // Double rAF garante que a transição CSS seja aplicada após o elemento entrar no DOM
    requestAnimationFrame(() => requestAnimationFrame(() => toast.classList.add('toast-visivel')));
    setTimeout(() => {
        toast.classList.remove('toast-visivel');
        toast.addEventListener('transitionend', () => toast.remove(), { once: true });
    }, 3500);
};
