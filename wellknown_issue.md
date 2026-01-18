# 既知の問題

## 1. ライブラリ選択時の黒いヘッダー表示

**ステータス**: 未解決（調査中）

**現象**:
スマホのライブラリで画像を選択する際、下からライブラリが出てくるときにページの高さに合わせた黒いヘッダーが表示される。

**発生箇所**:
- `CaptureEditScreen` の「ライブラリから選択」機能

**試した対策**:
1. `enableEdgeToEdge()` の追加 → 効果なし
2. `themes.xml` で Material3 テーマに変更 → 効果なし
3. ステータスバー・ナビゲーションバーの色を白に設定 → 効果なし
4. `PickVisualMedia` API（新しいPhoto Picker）への変更 → 効果なし
5. `window.decorView.setBackgroundColor(Color.WHITE)` の設定 → 効果なし
6. `WindowCompat.setDecorFitsSystemWindows()` の設定 → 効果なし

**関連ファイル**:
- `app/src/main/kotlin/space/suiten/presentation/screens/CaptureEditScreen.kt`
- `app/src/main/kotlin/space/suiten/MainActivity.kt`
- `app/src/main/res/values/themes.xml`

**考えられる原因**:
- システムのPhoto Pickerアクティビティの遷移時の挙動
- デバイス固有の問題の可能性
- Androidバージョンによる挙動の違い

**今後の調査方針**:
- 他のデバイスでの再現確認
- 他のアプリでの同様の挙動確認
- Android公式ドキュメントの Photo Picker 関連の調査
