# Mystcraft Unofficial NasukoCustom

Mystcraft Legacy の非公式 Minecraft **26.3 / NeoForge** 移植版です。
[ueno96941-Clown の 1.21.1 移植版](https://github.com/ueno96941-Clown/Mystcraft-Legacy-Unofficial-NeoForge-Port) を基にしています。

現在のソースコード: [Aoinasuko/MCMod_Mystcraft_Unofficial_NasukoCustom](https://github.com/Aoinasuko/MCMod_Mystcraft_Unofficial_NasukoCustom)。

- MOD名: **Mystcraft Unofficial NasukoCustom**
- バージョン: `0.1.0`
- Minecraft: **26.3**（26.1 / 26.2 / 1.21.1 用ではありません）
- NeoForge: **26.3.0.95** 以上の 26.3 系
- Java: **25**、64 bit
- 内部MOD ID: `mystcraft`。既存のアイテム・ページ・リソースIDを維持しています。
- JEI: 任意。使用する場合は Minecraft 26.3 用の **31.16** 系。

## 導入

`0.1.0` は、これまでの26.3移植と本のクリック・投棄の不具合修正を含むNasukoCustom版の最初のリリースです。旧バージョンのJARは外して差し替えてください。

Minecraft 26.3 に NeoForge 26.3 を導入し、`build/libs/mystcraft-unofficial-nasukocustom-0.1.0.jar` をインスタンスの `mods` フォルダーに入れてください。
同じ内部IDを使うため、旧MystcraftのJARは一緒に入れないでください。
26.3用の新しいワールドで使用してください。1.21.1版セーブの直接変換は検証対象に含めていません。

記述書・リンク書・ページ、書き物机・製本機・インクミキサー、書見台・クリスタルポータル、Ageの動的生成・保存などを引き継いでいます。
本の画面からのAge移動、投棄した本の表示、書き物机・製本機の画面表示と10件のGameTestを確認しています。互換性の制約は [KNOWN_ISSUES.md](KNOWN_ISSUES.md) を参照してください。

## ビルドと検証

JDK 25 をインストールし、`JAVA_HOME` を設定してください。Gradle Wrapper は同梱しています。
Windows向けスクリプトは `JAVA_HOME`、PATH、Gradleが取得したJDKの順にJava 25を探します。

```powershell
.\BUILD.bat
.\RUN_GAMETEST.bat
.\RUN_CLIENT_SMOKE_TEST.bat
```

通常の開発用起動は `RUN_CLIENT.bat` / `RUN_SERVER.bat` です。
`RUN_CLIENT_SMOKE_TEST.bat` は `run/saves/NasukoPortValidation` という専用ワールドを作成し、本でAgeへ移動して画面のスクリーンショットを保存した後、自動終了します。
サーバー利用時のMinecraft EULAは利用者が確認してください。

Gradleから直接実行する場合は `gradlew.bat build`、`gradlew.bat runGameTestServer`、`gradlew.bat runClientSmokeTest` を使用できます。
JDK 25でWindowsのUnix socketパスが長すぎる場合は `-Djdk.net.unixdomain.tmpdir=<短い作業用フォルダー>` を設定します。上記スクリプトは自動設定します。

## 出典とライセンス

原作: Mystcraft / Mystcraft Legacy、XCompWizおよびMystcraft開発者。
1.21.1移植: **ueno969 / ueno96941-Clown**（上流の連絡先: ueno96941@gmail.com）。
26.3移植版の名称: **Mystcraft Unofficial NasukoCustom**。

LGPL-3.0-only。原作者と上流移植者の表示・ライセンス・素材の出典を保持しています。
[ATTRIBUTION.md](ATTRIBUTION.md)、[MODIFICATION_NOTICE.md](MODIFICATION_NOTICE.md)、[SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md)、[LICENSE](LICENSE) を参照してください。
上流版のREADMEは [上流リポジトリのREADME](https://github.com/ueno96941-Clown/Mystcraft-Legacy-Unofficial-NeoForge-Port/blob/7e83df32feb5c68e1faee558daab2338ccadbb5d/README.md) で参照できます。既存の法的告知・監査報告は上流1.21.1版の記録を含み、26.3の動作保証を示すものではありません。

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

**This product contains trademarks and/or copyrighted works of Cyan. All rights reserved by Cyan. This product is not official and is not endorsed by Cyan.**

Mystcraft Org、XCompWiz、Mojang、Microsoft、NeoForged、Cyanによる公式版・承認版ではありません。
