# LATAM Any Wear — Landing Page Acadêmica

Projeto front-end responsivo criado para transformar o case fornecido em uma interface de apresentação jovem, simples e visual.

## Fonte do conteúdo
O conteúdo factual é baseado exclusivamente no documento do case fornecido:
- Any Wear, Anywhere, da Japan Airlines em parceria com Sumitomo.
- Proposta para a LATAM como serviço semelhante em viagens internacionais.
- Jornada descrita: reserva antecipada → entrega no hotel → utilização → devolução.
- Benefícios apresentados: menos bagagem, conveniência, possibilidade de receita complementar e sustentabilidade a ser mensurada.
- Riscos: operação/logística, higiene/qualidade, aceitação do consumidor, custos, imagem e necessidade de medir o impacto ambiental.
- O case não apresenta uma mecânica de pontos, bônus, desconto ou premiação promocional; por isso a página deixa esse item explicitamente como “não definido no case”.

## O que funciona
- Menu responsivo para mobile.
- Navegação entre campanha e catálogo de roupas.
- Filtro por categoria: jaquetas, calças, botas e toucas.
- Modal de detalhes conceituais.
- Botão de tela escura com persistência via `localStorage`.
- Layout adaptado a celular, notebook e desktop.
- Sem dependência de imagens externas.

## Tecnologias
- HTML5
- CSS3
- JavaScript para interações do navegador
- Python: servidor local de demonstração
- Java: servidor HTTP alternativo
- C: micro-servidor estático alternativo
- C++: micro-servidor estático alternativo

Os quatro backends são exemplos técnicos opcionais para demonstrar como a mesma interface pode ser hospedada em diferentes stacks. O navegador continua consumindo HTML/CSS/JS.

## Executar
### Opção simples — Python
```bash
python backend/server.py
```
Acesse `http://localhost:8000`.

### Java
```bash
javac backend/Server.java
java -cp backend Server
```

### C
```bash
cc backend/server.c -o backend/server
./backend/server
```

### C++
```bash
g++ -std=c++17 backend/server.cpp -o backend/server_cpp
./backend/server_cpp
```

> Os exemplos C/C++/Java usam a biblioteca padrão e foram mantidos pequenos para fins acadêmicos. Em produção, recomenda-se um servidor web dedicado ou framework adequado.

## Novidades da v2
- Seções novas: caso de referência, indicadores do piloto, FAQ e pré-reserva demonstrativa.
- API `POST /api/pre-reserva` (Python e Java, mesmo contrato). Grava em `data/*.jsonl`, sem pagamento nem preços.
- Portas: Python 8000, Java 8080 (ou defina `PORT`). Veja `AUDITORIA.md`.
- Os servidores C/C++ continuam apenas estáticos, sem a API.
