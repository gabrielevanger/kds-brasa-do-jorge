# Avisos de terceiros

## Material Symbols

Os ícones em `core/designsystem/src/main/res/drawable/ic_*.xml` são Material Symbols (estilo
Rounded, preenchido), do repositório [google/material-design-icons](https://github.com/google/material-design-icons),
distribuídos sob a [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

Alteração feita: remoção do atributo `android:tint="?attr/colorControlNormal"`, que depende do
AppCompat; a cor é aplicada pelo componente `Icon` do Compose. Os arquivos foram renomeados pelo
significado no KDS (por exemplo, `error` virou `ic_wait_late`).
