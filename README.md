# Fósseis pelo Mapa · 0.3

Aplicativo Android em Kotlin e Jetpack Compose para explorar um mapa, encontrar
fragmentos, reconstruir fósseis e aprender paleontologia.

## O que está implementado

- Mapa mundial com a API pública de tiles do OpenStreetMap (`https://tile.openstreetmap.org/{z}/{x}/{y}.png`), sem chave de API. Zoom de visão mundial à rua, arraste, cache HTTP e visual em marrom/âmbar. O serviço público tem regras de uso e capacidade limitada; não é uma API ilimitada.
- Personagens 3D feminino e masculino no mapa, com seleção salva no perfil.
- Celular com GPS como modo principal; emuladores Android iniciam automaticamente em **Modo PC**, com deslocamento por toque e achados do mundo virtual.
- GPS no celular: posição atualizada automaticamente com o app aberto, personagem acompanhando a caminhada, direção do movimento e mapa seguindo o jogador. Arrastar o mapa pausa o acompanhamento; o botão de centralizar o retoma. O modo Virtual mantém o deslocamento por toque para demonstração.
- Os personagens recebem um rig aproximado gerado por código e clipes `Idle` e `Walk`: pernas alternadas, joelhos flexionados, braços e tronco em movimento. Os arquivos originais continuam estáticos e preservados.
- Mapa como tela principal, com botão circular de fóssil no centro inferior para abrir os achados da coleção e retrato do perfil no canto inferior esquerdo. Laboratório e diário acessíveis pelo perfil ou pelas reticências.
- Achados virtuais em células geográficas fixas pelo mundo, atualizados conforme você anda. Os três dinossauros estão disponíveis em qualquer lugar nesta etapa; o raio de descoberta é de 400 m. A demonstração continua no Vale dos dinossauros.
- Tricerátops, T. rex e carnotauro com somente os crânios como achados no mapa. Os esqueletos são montados no laboratório após coletar e limpar o crânio. Nesta etapa, ele é a única peça coletável necessária; novos ossos serão adicionados depois.
- O laboratório mostra as peças coletadas imediatamente e libera a montagem somente com todas as peças coletáveis da espécie. Após montar, o modelo do esqueleto inteiro (que já inclui a cabeça) aparece no laboratório, na coleção e em RA.
- Catálogo ativo: tricerátops, T. rex e carnotauro, com os GLBs fornecidos pelo projeto. Amonite, trilobita, dente e osso ilustrativos foram retirados do catálogo, mapa, coleção e laboratório; arquivos e progresso antigos permanecem preservados.
- Coleta salva por ponto do mapa: o mesmo ponto não pode ser coletado outra vez. Outros pontos podem conter crânios repetidos, contabilizados na coleção, no diário e no nível do explorador. As montagens e os achados anteriores permanecem salvos.
- Descoberta em uma área de escavação virtual, com terreno texturizado, crânio GLB em destaque e informações científicas em um painel separado.
- Escavação com o crânio 3D por baixo de uma camada de terra: o pincel e a espátula removem sedimento com bordas suaves. Controles compactos de força, limpeza e integridade, ferramenta acompanhando o toque e poeira durante o gesto.
- A coleta exige pelo menos 93% de limpeza sem quebrar a peça. Uma tentativa quebrada pode ser reiniciada.
- GPS do Android, atualização enquanto a tela está ativa, raio de coleta de 100 m,
  precisão máxima de 100 m e sinal de no máximo 60 segundos.
- Coleção em galeria de duas colunas, com abas Coletados, Catálogo e Montados, imagens dos crânios e silhuetas das espécies ainda não encontradas. Progresso salvo localmente em SharedPreferences.
- Laboratório com seleção de espécie, visualização das peças em 3D, inventário de peças encontradas/faltantes e montagem completa salva localmente, inclusive após fechar o app.
- Modelos GLB locais com rotação, zoom e informações das partes.
- Inspeção da coleção com o 3D ocupando a maior parte da tela, enquadramento automático da peça visível e botão para reenquadrar. Informações científicas em um painel separado; a montagem abre o laboratório na espécie escolhida.
- Botão discreto **Peças** no detalhe do dinossauro: lista somente peças coletadas, com suas quantidades, e permite vê-las separadamente em 3D mesmo após a montagem. O atalho **Esqueleto** restaura o modelo completo; a câmera mostra a peça atualmente escolhida. Abrir o fóssil novamente mantém o esqueleto completo como apresentação inicial quando já montado.
- Câmera ao vivo com CameraX 1.5.3 e GLB sobreposto, disponível mesmo sem ARCore. Acesse pelo ícone de câmera no achado/coleção ou por **Ver na câmera** no menu.
- Realidade aumentada com SceneView 2.2.1 / ARCore nos aparelhos compatíveis: detecção de superfície horizontal, mira central, posicionamento por botão, reposicionamento, movimentação, rotação e escala.
- A câmera mostra o crânio antes da montagem e o esqueleto completo depois. Abrir a câmera durante a descoberta preserva a tentativa de limpeza.
- Menu de expedição com linhas simples, retratos compactos para escolher o personagem, modo Virtual/GPS e acesso direto à câmera.
- Diário com registros ilustrados e estado da montagem. Ajuda, informações do aplicativo, créditos e reinício da coleção ficam em painéis separados.

## Executar

Para abrir diretamente no emulador pelo terminal, sem abrir o Android Studio:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-emulator.ps1
```

O comando abre o AVD `Medium_Phone_API_35`, instala o APK existente e inicia o app.
Por padrão, usa o **Modo PC** e não depende da localização do Windows.
A sincronização com a posição do computador é opcional: acrescente
`-UseComputerLocation` para usar o serviço de localização do Windows. Uma fonte por IP é recusada porque não fornece
localização exata; fontes aceitas precisam informar erro de até 100 m.
Para essa opção, ative **Configurações → Privacidade e segurança → Localização** no Windows e selecione **Meu GPS** no menu do aplicativo.
Se o PC não tiver uma fonte precisa, use coordenadas obtidas no celular:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-emulator.ps1 -Latitude SUA_LATITUDE -Longitude SUA_LONGITUDE
```

Latitude e longitude usam ponto decimal. As coordenadas não são gravadas nos
arquivos de status/log. Um monitor oculto envia a posição ao emulador a cada
cinco segundos e encerra quando ele é fechado. Para reproduzir uma rota simulada
sem interferência desse monitor, abra o emulador com `-NoComputerLocation`.
Se houver sincronização anterior, reiniciar pelo script encerra somente o
monitor de localização deste projeto para o emulador escolhido.

Para um emulador já aberto pelo Android Studio, execute:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/set-emulator-location.ps1 -Follow
```

O comando aceita `-Serial emulator-5556` quando houver mais de um emulador e
`-Latitude` / `-Longitude` para definir um ponto manual. O GPS do emulador é
simulado; sua precisão não representa a precisão física informada pelo Windows.
Se a leitura falhar, o script informa a falha e não substitui a posição por uma
estimativa de cidade baseada no IP.

Se o Android solicitar depuração USB, clique em **Allow** na janela do emulador.
Para compilar as alterações antes de abrir, acrescente `-Build`. Para escolher
outro emulador instalado, acrescente `-Avd NomeDoEmulador`.

Abra a pasta no Android Studio, sincronize o Gradle e execute o módulo `app`.
Use Android 7.0 / API 24 ou superior e um aparelho com OpenGL ES 3.0.
O projeto utiliza o SDK Android 36 e as versões de ferramentas do catálogo
`gradle/libs.versions.toml`.

Para gerar o APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

O arquivo gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.
Para experimentar: **toque em um achado no mapa → inspecione o 3D → Começar a
limpeza → passe o pincel no sedimento → Guardar na coleção → Laboratório →
Montar esqueleto / Montar fóssil**. Toque no perfil ou nas reticências para alternar entre GPS e exploração virtual.

O pincel leve preserva a peça. A espátula remove mais sedimento, mas danifica o
fóssil quando atinge uma parte já exposta. Pincel com força muito alta também
pode causar dano. Os medidores mostram limpeza e integridade em tempo real.
Uma tentativa em andamento é preservada nas mudanças de configuração do Android.

No celular, o aplicativo inicia no modo **Meu GPS**. No emulador, inicia no **Modo PC**, sem pedir a localização física do computador. Permita localização precisa e mantenha
a localização do celular ativada. Com o app aberto, caminhe: a área de descoberta
é atualizada automaticamente e novos achados aparecem nos arredores. A coleta
exige sinal recente, precisão de até 100 m e proximidade de até 100 m do ponto.
Não há escolha de região ou botão de reinício da expedição no modo GPS.

As posições dos achados são determinísticas por célula geográfica: não se movem
com o jogador nem mudam ao reiniciar o aplicativo. A coleta de cada ponto fica
salva localmente. Não há servidor de multiplayer ou API de sítios fossilíferos:
os pontos são virtuais e não representam locais reais de extração.
Por enquanto, os três dinossauros podem aparecer em qualquer área; a distribuição
por região será uma etapa futura. Só os crânios possuem coleta nesta versão.

No emulador do Android Studio, abra **⋯ → Location**, envie uma posição ou
reproduza uma rota para simular o GPS. Isso não acompanha fisicamente o computador.
No emulador, **Modo PC** permite andar tocando no mapa, com novos achados em células do mundo ao se deslocar, sem o limite da área de demonstração. No celular, **Virtual** permite explorar a demonstração sem GPS.
No celular, conecte por USB, ative a depuração USB e selecione o aparelho no
Android Studio, ou instale o APK de debug gerado.

Referências: [localização no emulador](https://developer.android.com/studio/run/emulator-extended-controls#location)
e [política do serviço de mapas](https://operations.osmfoundation.org/policies/tiles/).
O app solicita somente tiles visíveis, identifica as requisições, respeita
cache/ETag/Expires e recua quando o provedor retorna 429/503. Não há download
prévio de cidades ou países. Para escalar a distribuição pública, será necessário
um provedor adequado ou infraestrutura própria de tiles.

O modo **Câmera** exige somente permissão de câmera e mostra o modelo sobre
a imagem ao vivo; nesse modo, ele não fica fixo no ambiente. No emulador, a
imagem vem da câmera configurada no AVD (virtual ou webcam).

O modo **RA** exige aparelho compatível com ARCore e Google Play Services para RA
instalado/atualizado. Aponte para uma mesa ou para o chão e mova o celular devagar.
Quando a mira detectar uma superfície, toque em **Posicionar aqui**. O modelo
permanece ancorado no ambiente; **Reposicionar** permite escolher outro ponto.
Se a RA estiver indisponível, o aplicativo oferece o modo Câmera. O botão
**Ativar RA** pode abrir a instalação do serviço em um aparelho compatível.
A coleção e o laboratório continuam funcionando sem acesso à câmera.

Referências: [prévia CameraX](https://developer.android.com/media/camera/camerax/preview)
e [requisitos ARCore](https://developers.google.com/ar/develop/java/enable-arcore).

## Modelos 3D

### Modelos integrados de `modelosGLB/`

Os oito arquivos fornecidos estão preservados na raiz. As cópias preparadas em
`app/src/main/assets/models/` reduzem o peso das malhas/texturas e convertem WebP
para PNG/JPEG compatíveis com o renderizador. Nenhum arquivo original foi alterado.
Os dinossauros aparecem nos arredores do jogador no GPS; no modo Virtual, ficam na expedição **Vale dos dinossauros**. Toque no perfil ou nas
reticências do mapa para escolher **Exploradora** ou **Explorador**. Essa escolha
permanece após fechar o aplicativo.

Para preparar novamente as cópias e as miniaturas (Node.js, Python, NumPy e Pillow):

```powershell
npm install --prefix .local-build/asset-tools @gltf-transform/core@4.3.0 @gltf-transform/extensions@4.3.0 @gltf-transform/functions@4.3.0 meshoptimizer@0.24.0
node scripts/prepare-models.mjs
python scripts/render-model-thumbnails.py
python scripts/render-explorer-portraits.py
```

`scripts/prepare-models.mjs` cria os arquivos individuais, as três composições
de museu e executa `scripts/animate-explorers.mjs` para animar os personagens.
Somente os crânios dos três dinossauros podem ser encontrados e limpos no mapa.
No laboratório, o crânio coletado aparece na bancada e libera **Montar esqueleto**.
A montagem inteira fica salva sem apagar os achados. Ao voltar à espécie, o
esqueleto completo continua visível. As composições de museu permanecem nos
assets; a visualização após montagem usa o modelo individual do corpo completo.
Os modelos são ilustrativos, não reconstruções anatômicas em escala científica.

O rig automático usa a posição dos vértices para atribuir pesos aos membros;
é uma aproximação e pode deformar roupas em algumas poses. Um rig feito no Blender
com clipes `Idle` e `Walk` pode substituí-lo nos mesmos caminhos
`explorer_female.glb` e `explorer_male.glb`. A origem/licença dos oito arquivos
fornecidos permanece registrada como não informada em `models/LICENSE.md`.

### Modelos ilustrativos originais

Os modelos incluídos são provisórios e estilizados, gerados originalmente para o
projeto sob CC0. Eles deixam o fluxo 3D e RA utilizável antes da criação dos assets
finais. Não representam espécimes identificados.

O perfil usa retratos de 512 px renderizados diretamente das cabeças dos GLBs,
em vez de ampliar as miniaturas do corpo. O botão da coleção usa a fotografia
**Ammonite Asteroceras**, de Dlloyd / Wikimedia Commons, sob **CC BY-SA 3.0**;
fonte e licença estão no Diário e em `app/src/main/assets/models/LICENSE.md`.
Os dados antigos de dente e osso continuam salvos, mas não aparecem na coleção
nem na contagem de fragmentos ativos. O nível já conquistado permanece.

Substitua os arquivos em `app/src/main/assets/models/` por GLBs com materiais e
texturas embutidos. Preserve os nomes dos nós de malha para a montagem e seleção:

| Arquivo | Nós das três partes |
| --- | --- |
| `ammonite.glb` | `core`, `chambers`, `rim` |
| `trilobite.glb` | `head`, `thorax`, `tail` |
| `tooth.glb` | `tip`, `crown`, `root` |
| `bone.glb` | `proximal`, `shaft`, `distal` |

Cada parte deve ser uma malha nomeada. Se o modelo tiver uma única malha,
separe-a em três objetos em um editor 3D, nomeie os objetos e exporte novamente.
Exporte com o centro do modelo na origem e eixo Y para cima. O app normaliza a
escala para a visualização e para um objeto de aproximadamente 30 cm em RA.
Mantenha baixo o número de polígonos e o tamanho das texturas para celulares.

Registre a licença e os créditos de cada substituição em
`app/src/main/assets/models/LICENSE.md` e atualize os créditos exibidos no Diário.
Para Sketchfab, use somente CC BY ou CC0 e inclua autor, link do modelo, licença
e alterações quando houver atribuição obrigatória.

Para regenerar os modelos provisórios:

```powershell
python scripts/generate_models.py
```

## Dados educativos e limites da V1

Os pontos e a distribuição por região são **simulações educativas**. Eles não
indicam sítios paleontológicos reais ou onde remover fósseis da natureza.
O catálogo descreve grupos e anatomia geral; não identifica uma espécie a partir
dos modelos ilustrativos. Não há conta, servidor, multiplayer, sincronização em
nuvem, inventário global de sítios ou validação contra adulteração do GPS.

O mapa usa internet para carregar apenas os tiles visíveis, com no máximo quatro
requisições simultâneas. O cache respeita o tempo de validade HTTP, ETag e
Last-Modified. Sem rede, aparece um cenário ilustrativo identificado como mapa de
expedição offline; os achados e o explorador continuam disponíveis. Esse cenário
não representa ruas reais. A atribuição do OpenStreetMap permanece visível.

Fontes usadas e vinculadas nas fichas:

- [Amonites — Natural History Museum](https://www.nhm.ac.uk/discover/what-is-an-ammonite.html)
- [Trilobitas — Natural History Museum](https://www.nhm.ac.uk/discover/how-trilobites-conquered-prehistoric-oceans.html)
- [Dentes de terópodes — Palaeontologia Electronica](https://www.palaeo-electronica.org/content/2019/2806-dental-features-in-theropods)
- [Fossilização — Natural History Museum](https://www.nhm.ac.uk/discover/how-are-fossils-formed.html)

Referências da implementação:
[SceneView 2.2.1](https://github.com/SceneView/sceneview-android/tree/v2.2.1),
[política de tiles do OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/).

No celular, o GPS exige a opção **Localização precisa** na permissão do Android.
Se o sinal ou a permissão não estiverem disponíveis, **Explorar sem GPS** abre o
modo Virtual. No PC, o mapa indica **PC · simulação**, e não apresenta uma
localização de IP como se fosse a posição física exata do computador.
