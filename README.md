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
本の画面からのAge移動、投棄した本の表示、書き物机・製本機の画面表示と、デスクの破壊・村人の就職・取引を含むGameTestを確認しています。互換性の制約は [KNOWN_ISSUES.md](KNOWN_ISSUES.md) を参照してください。

書見台用デスクはアーキビストの作業場です。無職の村人が就職でき、通常の村人と同じ取引画面でページ3種とブースターを購入できます。品切れ・作業場での補充・取引の保存は標準の村人の仕組みを使います。既存アーキビストの商品と在庫は初回の取引時に引き継ぎます。デスクの下段を壊すと本体1個、取り付けた上段があれば拡張1個が追加で落ちます。上段だけを壊すと拡張1個を落とします（クリエイティブではいずれも落としません）。

## 次元の怪物ページ

「次元の怪物」ページ（`mystcraft:DimensionalMonsters`）を記述書に入れてAgeを作ると、そのAgeの敵Mobの自然スポーン候補に次の種が枚数に応じて加わります。暗さ・明かり・プレイヤーからの距離・敵Mob上限は通常の自然スポーンと共通です。ピースフルでは出現しません。他のAgeやオーバーワールドには影響しません。

| 必要枚数 | Mob | HP | 攻撃力 | アーマー | 行動 |
| --- | --- | --- | --- | --- | --- |
| 1枚以上 | 次元の翼竜 | 40（❤×20） | 6 | 4 | 羽ばたきながら高さを合わせ、約1.25秒溜めて直線突進 |
| 1枚以上 | 次元の蛇 | 30（❤×15） | 6＋毒I・10秒 | 0 | 地上を素早く移動し、飛びついて攻撃 |
| 2枚以上 | 次元の妖精 | 40（❤×20） | 6＋浮遊III・3秒 | 2 | 紫色の翼で飛行。テレポート→5秒待機→壁を貫くレーザー→2秒待機を反復 |
| 2枚以上 | 次元のワーム | 60（❤×30） | 8 | 6 | 大型の地中突進。窒息無効。足場のない場所へは進めない |
| 3枚以上 | 次元の災禍 | 100（❤×50） | 10 | 10 | 高さ3mの巨大な悪魔。飛行・5方向への爆発弾・魔法陣からの召喚 |

自然スポーンの重みは翼竜8・蛇12・妖精5・ワーム6・災禍1です。災禍は低確率の枠です。妖精は最初の発見に視線が必要ですが、その後は壁の向こうの相手も追跡します。レーザーは1発につき1回ダメージを与えます。

災禍の弾は半径3（通常クリーパーと同等）の爆発を起こし、地形破壊は `mobGriefing` に従います。爆発の対Entityダメージは最大10です。召喚は周囲24ブロックの次元モンスター（災禍自身と他の災禍を除外）が5体以下のとき、3か所の魔法陣を2秒間表示して最大3体を呼びます。再使用間隔は15秒、召喚後の仲間は最大8体に制限し、災禍の再帰召喚と術者死亡後の召喚を防ぎます。

不安定度の軽減量は追加する1枚ごとに **500、250、125、62、31、15、7、3、1**（合計最大994）です。10枚目以降の追加軽減はありません。各種のスポーン重みは一定ですが、2枚目・3枚目で危険な種が追加されます。ページは既存のページ入手・複写・製本の仕組みとクリエイティブタブで利用できます。5種すべてのスポーンエッグも追加しています。

モデル・テクスチャはBlender MCPで新規制作しました。編集元は `art/dimensional_monsters.blend` と `art/dimensional_reinforcements.blend`、再生成スクリプトは `tools/blender_dimensional_monsters.py` と `tools/blender_dimensional_reinforcements.py` です。

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
