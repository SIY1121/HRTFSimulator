# HRTF Simulator

![HRTF Simulator 2019_08_28 13_52_24](https://user-images.githubusercontent.com/17082836/63826667-2f2b6e00-c99b-11e9-9ecb-c640871dca5c.png)

HRTF(頭部伝達関数)を利用した立体音響のリアルタイム生成のデモ。

# 動作環境
- Java8以上(JavaFXが利用できる必要があります)
- [こちら](http://www.sp.m.is.nagoya-u.ac.jp/HRTF/index-j.html)よりHRTFデータベースのダウンロードが必要です

# 実行
プロジェクトルートで以下のコマンドを実行してください。
```bash
./gradlew jfxRun
```

## HRTFの選択
DLしたHRTFで、`elev0, elev5 ....elev90` というフォルダが 含まれているフォルダ を選択してください。

## 音源の選択
立体音響化したい音声ファイルを選択してください。(FFmpegが対応している形式を読み込めます)

## 再生
再生ボタンで再生されます。
スライダーを動かして音源の位置を変更できます。
