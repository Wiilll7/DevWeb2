/* Funções comuns a todas as páginas (JavaScript puro, sem frameworks). */
(function () {
    // URL raiz da aplicação (funciona com qualquer context path), terminada em "/"
    var BASE = document.currentScript.src.replace(/js\/app\.js(\?.*)?$/, '');

    /** Chama a API. dados (objeto) vai como formulário urlencoded. Lança Error(mensagem) em caso de erro. */
    async function api(metodo, caminho, dados) {
        var opt = { method: metodo, credentials: 'same-origin', headers: { 'X-Requested-With': 'fetch' } };
        if (dados) {
            opt.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
            opt.body = new URLSearchParams(dados).toString();
        }
        var r = await fetch(BASE + 'api/' + caminho, opt);
        var corpo = null;
        try { corpo = await r.json(); } catch (e) { /* sem corpo */ }
        if (r.status === 401 && caminho !== 'login') {
            location.href = BASE + 'login.html';
            throw new Error('Sessão expirada.');
        }
        if (!r.ok) throw new Error((corpo && corpo.erro) || ('Erro ' + r.status));
        return corpo;
    }

    /** Cria elemento: h('a', {href:'x'}, 'texto', outroNo). Usa textContent => sem risco de XSS. */
    function h(tag, attrs) {
        var e = document.createElement(tag);
        for (var k in (attrs || {})) {
            if (k === 'onclick') e.addEventListener('click', attrs[k]);
            else if (attrs[k] !== false && attrs[k] != null) e.setAttribute(k, attrs[k]);
        }
        for (var i = 2; i < arguments.length; i++) {
            var f = arguments[i];
            if (f == null) continue;
            e.appendChild(typeof f === 'object' ? f : document.createTextNode(String(f)));
        }
        return e;
    }

    function limpar(el) { while (el.firstChild) el.removeChild(el.firstChild); }

    function mensagem(el, texto, erro) {
        el.textContent = texto || '';
        el.style.color = erro ? '#c00' : '#080';
    }

    function dataHora(iso) {
        var d = new Date(iso);
        return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    }
    function dataBR(iso) { var p = iso.split('-'); return p[2] + '/' + p[1] + '/' + p[0]; }
    function moeda(v) { return Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }); }

    /** Monta o menu do topo. s = sessão (ou null para visitante). */
    function menu(s) {
        var ul = h('ul');
        ul.appendChild(h('li', null, h('a', { href: BASE + 'index.html' }, 'Notícias')));
        if (s && s.autenticado) {
            ul.appendChild(h('li', null, h('a', { href: BASE + 'app/noticias.html' }, 'Minhas notícias')));
            ul.appendChild(h('li', null, h('a', { href: BASE + 'app/contas.html' }, 'Contas')));
            if (s.perfil === 'ADMIN') ul.appendChild(h('li', null, h('a', { href: BASE + 'app/usuarios.html' }, 'Usuários')));
            ul.appendChild(h('li', null, h('a', {
                href: '#', onclick: async function (ev) {
                    ev.preventDefault();
                    await api('POST', 'logout', {});
                    location.href = BASE + 'login.html';
                }
            }, 'Sair (' + s.login + ')')));
        } else {
            ul.appendChild(h('li', null, h('a', { href: BASE + 'login.html' }, 'Entrar')));
        }
        var nav = document.getElementById('menu');
        limpar(nav);
        nav.appendChild(h('a', { href: BASE + 'index.html' }, h('h2', null, 'Portal de Notícias')));
        nav.appendChild(ul);
        if (s && s.autenticado) {
            var info = document.getElementById('contexto');
            if (info) info.textContent = s.nome + ' — ' + s.empresaNome + ' (' + (s.perfil === 'ADMIN' ? 'Administrador' : 'Usuário comum') + ')';
        }
    }

    /** Para páginas privadas: garante login (e perfil ADMIN se exigido). Devolve a sessão ou null (já redirecionou). */
    async function exigirLogin(somenteAdmin) {
        var s = await api('GET', 'sessao');
        if (!s.autenticado) { location.href = BASE + 'login.html'; return null; }
        if (somenteAdmin && s.perfil !== 'ADMIN') { location.href = BASE + 'app/noticias.html'; return null; }
        menu(s);
        return s;
    }

    window.App = { BASE: BASE, api: api, h: h, limpar: limpar, mensagem: mensagem, dataHora: dataHora,
                   dataBR: dataBR, moeda: moeda, menu: menu, exigirLogin: exigirLogin };
})();
