# 対応レポート

## 2024-01-13 バグ修正

### 1. ライブラリ選択時の黒いヘッダー問題
**問題**: スマホのライブラリで画像を選択すると謎の黒いヘッダーが表示される

**対応内容**:
- `MainActivity.kt`に`enableEdgeToEdge()`を追加
- ステータスバーの表示を適切に制御するように修正

**修正ファイル**:
- `app/src/main/kotlin/space/suiten/MainActivity.kt`

---

### 2. メモがキャプチャに反映されない問題
**問題**: メモを追加してもセッション（キャプチャ）に反映されない

**対応内容**:
- `CaptureViewModel`に`createCaptureForBook()`メソッドを追加
- 本に対してキャプチャを作成する際、自動でセッションを作成する機能を実装
- `SuiteNNavHost`で`CaptureEditScreen`の`onSubmit`コールバックを実装し、実際にキャプチャを保存するように修正

**修正ファイル**:
- `app/src/main/kotlin/space/suiten/presentation/viewmodel/CaptureViewModel.kt`
- `app/src/main/kotlin/space/suiten/presentation/navigation/SuiteNNavHost.kt`

---

### 3. 最近の読書に本が反映されない問題
**問題**: 本棚で追加した本がホーム画面の「最近の読書」に表示されない

**対応内容**:
- `HomeScreen`を静的表示から動的データ表示に変更
- `BookViewModel`から本の一覧を取得して表示
- `CaptureViewModel`から未処理キャプチャを取得して表示
- `SuiteNNavHost`でHomeScreenにデータを渡すように修正

**修正ファイル**:
- `app/src/main/kotlin/space/suiten/presentation/screens/HomeScreen.kt`
- `app/src/main/kotlin/space/suiten/presentation/navigation/SuiteNNavHost.kt`

---

## その他の改善

### BookDetailScreen の改善
- ReactのUIに合わせて「読書セッション」から「キャプチャ一覧」表示に変更
- FABに「気づきを追加」ラベルを追加
- キャプチャカードに感情スタンプ・メモ・ステータスを表示

### CaptureEditScreen の改善
- 感情スタンプ選択機能（💡ひらめき、🤔疑問、⚠️注意、🔥重要）
- メモ入力機能（100文字制限）
- カメラ撮影機能（権限リクエスト対応）
- ライブラリから画像選択機能
- プライバシー注意書きの表示

### データ層の改善
- `CaptureDao`に`getCapturesByBookId()`メソッドを追加
- `CaptureRepository`に本に紐づくキャプチャ取得機能を追加
