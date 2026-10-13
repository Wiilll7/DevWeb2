# Portal multi-tenant com Chinese Wall Policy (CWP)

Projeto Maven/WAR (Servlets `javax`, Java 8, Tomcat 9) construído sobre o exemplo dos animais.
Frontend: **HTML puro + JavaScript puro** (sem framework, sem JSP), usando o mesmo `mvp.css`.

## Como rodar
1. Abra no NetBeans (ou `mvn package`) e implante em um Tomcat 9 (contexto `/DevWeb_2`).
2. Acesse `http://localhost:8080/DevWeb_2/`.
3. Na primeira execução o banco é criado em `~/devweb_multitenant.mv.db` (H2 embarcado, sem instalação)
   e carregado com dados de demonstração (classe `controle/Inicializador`).

Para usar outro banco: `-Ddb.url=... -Ddb.user=... -Ddb.pass=...` (ou `DB_URL`, `DB_USER`, `DB_PASS`) e o driver no `pom.xml`.

## Usuários de demonstração (senha de todos: `123456`)
| Usuário | Empresas / perfil |
|---|---|
| admin  | Banco Alfa, Tech Gama, Petro Delta — **ADMIN** |
| ana    | Banco Beta — **ADMIN** |
| carlos | Petro Épsilon — **ADMIN** |
| joao   | Banco Alfa — comum |
| maria  | Banco Beta e Tech Gama — comum |

Conflitos de interesse cadastrados: **Banco Alfa × Banco Beta** e **Petro Delta × Petro Épsilon**.

## Manutenção de empresas (direto no banco)
Tabela `empresa(id, nome, conflitos)`. O campo `conflitos` lista os ids das empresas em conflito, separados por vírgula.
O conflito é simétrico: basta uma das duas empresas citar a outra.
```sql
INSERT INTO empresa (nome, conflitos) VALUES ('Banco Gama', '1,2');   -- conflita com as empresas 1 e 2
UPDATE empresa SET conflitos = '2,6' WHERE id = 1;
```
Console do H2: `java -cp h2-2.2.224.jar org.h2.tools.Console` (URL `jdbc:h2:~/devweb_multitenant`, usuário `sa`, senha vazia;
com a aplicação rodando use `;AUTO_SERVER=TRUE` na URL).

## Modelo de dados
`empresa` · `usuario` (login único) · `usuario_empresa` (usuário × empresa, com **perfil COMUM/ADMIN naquela empresa**) ·
`noticia` e `conta` (ambas com `empresa_id`). Veja `src/main/resources/schema.sql`.

## Como cada requisito é atendido
| Requisito | Onde |
|---|---|
| Login com empresa + usuário + senha | `LoginServlet`, `login.html`. O usuário precisa ter vínculo com a empresa informada. |
| Só altera dados da própria empresa | A empresa **vem da sessão, nunca do cliente**; todo `UPDATE/DELETE/SELECT` de notícia e conta filtra por `empresa_id` (`NoticiaDao`, `ContaDao`). |
| Comum não acessa cadastro de usuários | `AuthFilter` bloqueia `/api/admin/*` e `/app/usuarios.html`; `UsuarioServlet` confere de novo no banco. |
| Admin só cadastra usuários das empresas que administra | `UsuarioServlet`: o escopo (empresas em que é ADMIN) é lido do banco a cada chamada; qualquer empresa fora dele é recusada. |
| Sem conflito de interesses (CWP) | `util/Cwp`. Validado (1) ao cadastrar/editar usuário — o conjunto final de empresas do usuário não pode conter conflito, inclusive com vínculos que ele já tenha em empresas que o admin não vê — e (2) **no login**, caso o conflito tenha sido criado depois, direto no banco. |
| Notícias públicas com o nome da empresa | `index.html` + `GET /api/noticias` (sem login). |
| Sem cadastro de empresas | Só leitura (`EmpresaDao`); manutenção no banco. |

### Decisões que vale conhecer
- A regra CWP vale para **todos** os usuários (não só admins): ninguém pode ter vínculo com duas empresas concorrentes.
- Um admin **não consegue alterar nome/senha** de um usuário que também pertence a empresa que ele não administra
  (senão poderia assumir a conta dele nessa outra empresa); só altera os vínculos com as suas empresas.
- Um admin não edita o próprio cadastro pela tela. "Remover" tira o usuário das empresas do admin; se não sobrar nenhuma, o usuário é excluído.
- O login é único no sistema (um usuário pode pertencer a várias empresas não conflitantes); por isso o admin não consegue
  "adicionar" à sua empresa um login que já existe — só cadastrar logins novos.

## Segurança básica incluída
Senhas com PBKDF2-SHA256 + salt; sessão nova a cada login (anti session fixation); cookie HttpOnly + SameSite=Lax;
mensagem de erro de login genérica; vínculo revalidado a cada requisição (remover o acesso derruba a sessão);
cabeçalho `X-Requested-With` obrigatório em requisições que alteram dados (anti-CSRF); páginas montadas com `textContent` (anti-XSS);
SQL sempre com `PreparedStatement`.

## Estrutura
```
src/main/java/modelo        Empresa, Usuario, Sessao, Noticia, Conta
src/main/java/persistencia  Db, EmpresaDao, UsuarioDao, NoticiaDao, ContaDao
src/main/java/util          Senha (PBKDF2), Cwp (política), Json
src/main/java/controle      AuthFilter, LoginServlet, EmpresaServlet, NoticiaServlet, ContaServlet, UsuarioServlet, Inicializador
src/main/webapp             index.html, login.html, js/app.js, app/{noticias,contas,usuarios}.html
```
