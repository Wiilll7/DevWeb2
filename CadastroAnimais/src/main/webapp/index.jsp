<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%@page import="modelo.Cachorro"%>
<%@page import="modelo.Gato"%>
<%@page import="modelo.Coelho"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Cadastro de Animal</title>
    </head>
    <body>
        <form method="POST" action="index.jsp">
            <h1>Adicionar animal: </h1>
            
            <p>Nome: <input type="text" name="nome" required></p>
            <p>Altura: <input type="text" name="altura" required></p>
            
            <p>
                <label for="animal"> Escolha um animal: </label>
                <select name="tipo" id="animal">
                    <option value="cachorro">Cachorro</option>
                    <option value="gato">Gato</option>
                    <option value="coelho">Coelho</option>
                </select>
            </p>
            
            <button type="submit">Salvar</button>
        </form>

        <hr>
        
        <%
            String nome = request.getParameter("nome");
            String altura = request.getParameter("altura");
            //Double altura = Double.valueOf(aaltura);
            String tipo = request.getParameter("tipo");
            
            if (nome != null && altura != null && tipo != null) {
                
                Cachorro c = null;
                Gato g = null;
                Coelho co = null;
                
                if (tipo.equals("cachorro")) {
                    c = new Cachorro();
                    c.setNome(nome);
                    //c.setAltura(altura);
                    
                } else if (tipo.equals("gato")) {
                    g = new Gato();
                    g.setNome(nome);
                    //g.setAltura(altura);
                    
                } else if (tipo.equals("coelho")) {
                    co = new Coelho();
                    co.setNome(nome);
                    //co.setAltura(altura);
                }
                
                
                if (c != null) {
                    
                } else if (g != null) {
                    
                } else if (co != null) {
                    
                }
            }
        %>
        
        
    </body>
</html>