# 把 Soreada 上傳到 GitHub（新手一步一步）

你現在已經完成的是：**在 GitHub 建立了空 repo**。  
本機的程式、commit、tag、README、LICENSE 都已經準備好了。

> 先不要照 GitHub 頁面上「Quick setup」的 `echo "# Soreada" >> README.md`、`git init`、`git add`、`git commit` 做。  
> 那些是給「完全沒有本機專案」的人用的；你的專案已經有完整的 commit，照那個做會多出一個空 commit，沒有必要。

目前狀態：

- 本機資料夾：`/Users/kanae/Documents/DSH/foldpage`
- 分支：`main`
- 最新 commit：以 `git log -1 --oneline` 顯示的為準
- 已有 tag：`v0.1.0`、`v0.2.0`、`v0.3.0`
- GitHub repo：`https://github.com/Amamiya-Kanae/Soreada`
- 本機已經接好 `origin`

---

## 第 1 步：建立一組「上傳用的密碼」（Personal Access Token）

GitHub 現在不接受帳號密碼直接 push，要用 **Personal Access Token（PAT）**。只需要做一次。

1. 在 GitHub 右上角點你的頭像 → **Settings**。
2. 左側選單拉到最下面 → **Developer settings**。
3. 點 **Personal access tokens** → **Tokens (classic)**。
4. 右上角 **Generate new token** → **Generate new token (classic)**。
5. 填寫：
   - **Note**：`Soreada upload`
   - **Expiration**：`30 days`
   - **Select scopes**：勾選 **`repo`**（整張 `repo` 大項都要勾）
6. 拉到最下面 → **Generate token**。
7. 複製畫面出現的 token（通常以 `ghp_` 開頭）。

⚠️ 這個 token 只會顯示一次。  
⚠️ 不要把 token 貼到對話、README、commit 或截圖裡。  
⚠️ 如果外洩，回到同一個頁面按 **Revoke** 刪掉，再建一個新的。

---

## 第 2 步：打開 Terminal

1. 按鍵盤 `Command` + `Space`。
2. 輸入 `Terminal`，按 Enter。
3. 會看到一個文字視窗，最後面有 `%` 或 `$`，這就是可以輸入指令的地方。

複製下面這一行，貼到 Terminal，按 Enter：

```bash
cd /Users/kanae/Documents/DSH/foldpage
```

確認你在正確的資料夾：

```bash
pwd
```

應該顯示：

```text
/Users/kanae/Documents/DSH/foldpage
```

再看一次 remote：

```bash
git remote -v
```

應該看到：

```text
origin	https://github.com/Amamiya-Kanae/Soreada.git (fetch)
origin	https://github.com/Amamiya-Kanae/Soreada.git (push)
```

如果沒有，才需要執行：

```bash
git remote add origin https://github.com/Amamiya-Kanae/Soreada.git
```

---

## 第 3 步：把 main 推上去

輸入：

```bash
git push -u origin main
```

接下來 Terminal 會問你：

```text
Username for 'https://github.com':
```

輸入你的 GitHub 使用者名稱：

```text
Amamiya-Kanae
```

按 Enter。

接著會問：

```text
Password for 'https://Amamiya-Kanae@github.com':
```

這裡**不是**輸入 GitHub 登入密碼，而是貼上第 1 步複製的 **token**。

- 在 Terminal 按 `Command` + `V` 貼上。
- 貼上時畫面**不會顯示任何字元**，這是正常的。
- 貼好後按 Enter。

成功時會看到類似：

```text
Enumerating objects: ...
Writing objects: 100% ...
To https://github.com/Amamiya-Kanae/Soreada.git
 * [new branch]      main -> main
branch 'main' set up to track 'origin/main'.
```

這時候回到瀏覽器，重新整理 GitHub repo 頁面，應該會看到 `README.md`、`LICENSE`、`app/`、`docs/` 等檔案。

---

## 第 4 步：把版本 tag 推上去

```bash
git push origin --tags
```

成功時會看到：

```text
 * [new tag]         v0.1.0 -> v0.1.0
 * [new tag]         v0.2.0 -> v0.2.0
 * [new tag]         v0.3.0 -> v0.3.0
```

重新整理 GitHub 頁面，左側 **Releases** 會看到 tag。

---

## 第 5 步：設定 repo 的 Description 和 Topics

在 repo 首頁右側找 **About**（如果沒有，點右邊的齒輪 **Edit repository details**）。

**Description** 貼上：

```text
免費、開源、現代的 EPUB/TXT 閱讀器，支援摺疊屏與平板寬屏 · 免费、开源、现代的 EPUB/TXT 阅读器，支持折叠屏与平板宽屏 · A free, open-source, modern EPUB/TXT reader for foldables and tablets
```

**Topics** 一個一個加，或貼上：

```text
android
epub
txt
reader
jetpack-compose
material3
foldable
tablet
open-source
```

按 **Save changes**。

---

## 第 6 步：發佈 APK 到 Releases

APK 不要用「Add file → Upload files」上傳到程式碼裡；要放在 **Releases**。

1. 在 repo 首頁右側點 **Releases**。
2. 點 **Create a new release**。
3. **Choose a tag** 選 `v0.3.0`。
4. **Release title** 填：

   ```text
   Soreada 0.3.0
   ```

5. **Describe this release** 貼上：

   ```text
   首個公開版本：EPUB/TXT、摺疊屏與平板雙頁、護眼、紙張顆粒、雙指縮放、書籤、搜尋、三語介面。
   ```

6. 拉到下面 **Attach binaries by dropping them here or selecting them**。
7. 點 **selecting them**，或直接打開 Finder 拖曳：
   - Finder → 按 `Command` + `Shift` + `G`
   - 貼上：`/Users/kanae/Documents/DSH/foldpage/dist`
   - 把 **`Soreada-0.3.0.apk`** 拖進 GitHub 的附件框。
8. 等檔案上傳完，按 **Publish release**。

完成後 Releases 頁面會有 APK 下載按鈕；README 的下載連結也會指向這裡。

---

## 第 7 步：檢查

回到 repo 首頁，確認：

- 上方有 Description。
- Topics 有出現。
- README 的圖片正常顯示。
- 右側 Releases 有 `Soreada 0.3.0`。
- 點進 Releases 可以下載 APK。

---

## 常見錯誤

### `remote origin already exists`

代表已經接過 remote，不用再加。直接：

```bash
git remote set-url origin https://github.com/Amamiya-Kanae/Soreada.git
```

### `Authentication failed` 或 `Support for password authentication was removed`

- 你輸入的是 GitHub 帳號密碼，不是 token。
- 重新執行 `git push -u origin main`，密碼欄位貼上 `ghp_...` token。
- 如果 token 過期，重新建一個。

### `Repository not found`

- 確認 repo 名稱和帳號完全正確：
  `https://github.com/Amamiya-Kanae/Soreada.git`
- 確認 repo 已經建立。
- 確認 token 有 `repo` 權限。

### `Everything up-to-date`，但 GitHub 還是空的

可能推到錯的 remote。執行：

```bash
git remote -v
git branch
```

應該看到 `origin` 指向 `Amamiya-Kanae/Soreada`，分支是 `main`。

### 貼上 token 後完全沒反應

Terminal 貼上密碼時不會顯示字元，這是正常的。直接按 Enter。

### 卡住了

把 Terminal 最後 5 行貼給我，**但不要把 token 貼出來**。我看到錯誤訊息就能帶你下一步。
