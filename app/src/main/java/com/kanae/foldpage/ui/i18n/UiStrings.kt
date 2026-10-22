package com.kanae.foldpage.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.kanae.foldpage.data.AppLanguage
import java.util.Locale

enum class UiKey {
    APP_NAME,
    BOOKS_COUNT,
    BOOKS_COUNT_ONE,
    SETTINGS_DESC,
    SEARCH_DESC,
    CLOSE_SEARCH,
    SEARCH_HINT,
    NO_RESULTS,
    NO_RESULTS_HINT,
    IMPORT_BOOKS,
    IMPORTING,
    CHOOSE_FILE,
    SUPPORTED_FORMATS,
    EMPTY_TITLE,
    EMPTY_SUBTITLE,
    MORE_OPTIONS,
    CONTINUE_READING,
    RESTART_READING,
    REMOVE_FROM_LIBRARY,
    COVER_OF,
    READ_PERCENT,
    REMOVE_TITLE,
    REMOVE_MESSAGE,
    REMOVE,
    CANCEL,
    RESTART_TITLE,
    RESTART_MESSAGE,
    RESTART,
    IMPORTED_BOOK,
    REMOVED_BOOK,
    LIBRARY_LOAD_FAILED,
    LOADING_LIBRARY,
    DELETE_FAILED,
    RESET_FAILED,
    LOADING_BOOK,
    CANNOT_OPEN,
    RETRY,
    BACK_TO_LIBRARY,
    RENDERING,
    NO_CONTENT,
    PAGE_LOAD_FAILED,
    RETRY_PAGE,
    READING_SETTINGS,
    PREVIOUS_PAGE,
    NEXT_PAGE,
    PAGE_OF,
    PAGES_OF,
    PAGE_SHORT,
    PAGE_NUMBER,
    BOOKMARK,
    ADD_BOOKMARK,
    REMOVE_BOOKMARK,
    BOOKMARK_AT,
    READING_DIRECTION,
    AUTO,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    DIRECTION_AUTO_HINT,
    DIRECTION_MANUAL_HINT,
    DIRECTION_LTR_TEXT,
    DIRECTION_RTL_TEXT,
    PAGE_LAYOUT,
    SINGLE_PAGE,
    DOUBLE_PAGE,
    LAYOUT_HINT,
    FONT_SIZE,
    FONT_CURRENT,
    IMAGE_BOOK_NOTE,
    DISPLAY_EFFECTS,
    EYE_CARE,
    EYE_CARE_HINT,
    PAPER_GRAIN,
    PAPER_GRAIN_HINT,
    STRENGTH,
    ZOOM_HINT,
    GLOBAL_SETTINGS,
    SETTINGS_TITLE,
    LANGUAGE,
    TRADITIONAL_CHINESE,
    SIMPLIFIED_CHINESE,
    ENGLISH,
    THEME,
    THEME_SYSTEM,
    THEME_LIGHT,
    THEME_DARK,
    READER_WHITE_NOTE,
    ABOUT,
    ABOUT_SUMMARY,
    DEVELOPER,
    VERSION,
    GITHUB,
    CLOSE,
    ERROR_FILE_READ,
    ERROR_NOT_EPUB,
    ERROR_EMPTY_FILE,
    ERROR_TOO_LARGE,
    ERROR_UNSUPPORTED,
    ERROR_EPUB_CONTAINER,
    ERROR_EPUB_OPF,
    ERROR_EPUB_NO_SPINE,
    ERROR_EPUB_NO_CONTENT,
    ERROR_TXT_EMPTY,
    ERROR_BOOK_MISSING,
    ERROR_FILE_MISSING,
    ERROR_UNKNOWN,
    ERROR_STORAGE,
    READER_GESTURE_HINT,
}

class UiStrings(val language: AppLanguage) {
    private val values: Map<UiKey, String> = when (language) {
        AppLanguage.TRADITIONAL -> traditional
        AppLanguage.SIMPLIFIED -> simplified
        AppLanguage.ENGLISH -> english
    }

    fun get(key: UiKey, vararg args: Any): String {
        val template = values[key] ?: key.name
        return if (args.isEmpty()) template else String.format(Locale.ROOT, template, *args)
    }

    private companion object {
        val traditional = mapOf(
            UiKey.APP_NAME to "蘇打閱讀",
            UiKey.BOOKS_COUNT to "共 %1\$d 本書",
            UiKey.BOOKS_COUNT_ONE to "共 1 本書",
            UiKey.SETTINGS_DESC to "設置",
            UiKey.SEARCH_DESC to "搜尋書籍",
            UiKey.CLOSE_SEARCH to "關閉搜尋",
            UiKey.SEARCH_HINT to "搜尋書名或作者",
            UiKey.NO_RESULTS to "找不到符合的書",
            UiKey.NO_RESULTS_HINT to "試試其他關鍵字。",
            UiKey.IMPORT_BOOKS to "匯入書籍",
            UiKey.IMPORTING to "正在匯入書籍…",
            UiKey.CHOOSE_FILE to "選擇檔案",
            UiKey.SUPPORTED_FORMATS to "支援 .epub 與 .txt",
            UiKey.EMPTY_TITLE to "書庫還是空的",
            UiKey.EMPTY_SUBTITLE to "匯入 EPUB 漫畫或 TXT 小說，\n直向一頁、橫向兩頁，慢慢看。",
            UiKey.MORE_OPTIONS to "《%1\$s》更多選項",
            UiKey.CONTINUE_READING to "繼續閱讀",
            UiKey.RESTART_READING to "從第一頁重新開始",
            UiKey.REMOVE_FROM_LIBRARY to "從書庫移除",
            UiKey.COVER_OF to "《%1\$s》封面",
            UiKey.READ_PERCENT to "已讀 %1\$d%%",
            UiKey.REMOVE_TITLE to "移除這本書？",
            UiKey.REMOVE_MESSAGE to "《%1\$s》會從書庫移除。你手機上的原始檔案不受影響。",
            UiKey.REMOVE to "移除",
            UiKey.CANCEL to "取消",
            UiKey.RESTART_TITLE to "從第一頁重新開始？",
            UiKey.RESTART_MESSAGE to "《%1\$s》目前的閱讀進度會被清除。",
            UiKey.RESTART to "重新開始",
            UiKey.IMPORTED_BOOK to "已匯入《%1\$s》",
            UiKey.REMOVED_BOOK to "已移除《%1\$s》",
            UiKey.LIBRARY_LOAD_FAILED to "讀取書庫失敗。",
            UiKey.LOADING_LIBRARY to "正在整理書庫…",
            UiKey.DELETE_FAILED to "移除失敗。",
            UiKey.RESET_FAILED to "無法重設閱讀進度。",
            UiKey.LOADING_BOOK to "正在載入書籍…",
            UiKey.CANNOT_OPEN to "無法開啟這本書",
            UiKey.RETRY to "再試一次",
            UiKey.BACK_TO_LIBRARY to "返回書庫",
            UiKey.RENDERING to "正在排版…",
            UiKey.NO_CONTENT to "沒有可顯示的內容",
            UiKey.PAGE_LOAD_FAILED to "第 %1\$d 頁載入失敗",
            UiKey.RETRY_PAGE to "重試",
            UiKey.READING_SETTINGS to "閱讀設定",
            UiKey.PREVIOUS_PAGE to "上一頁",
            UiKey.NEXT_PAGE to "下一頁",
            UiKey.PAGE_OF to "第 %1\$d 頁 / 共 %2\$d 頁",
            UiKey.PAGES_OF to "第 %1\$d–%2\$d 頁 / 共 %3\$d 頁",
            UiKey.PAGE_SHORT to "%1\$d / %2\$d",
            UiKey.PAGE_NUMBER to "第 %1\$d 頁",
            UiKey.BOOKMARK to "書籤",
            UiKey.ADD_BOOKMARK to "加入書籤",
            UiKey.REMOVE_BOOKMARK to "移除書籤",
            UiKey.BOOKMARK_AT to "書籤：第 %1\$d 頁",
            UiKey.READING_DIRECTION to "閱讀方向",
            UiKey.AUTO to "自動",
            UiKey.LEFT_TO_RIGHT to "左至右",
            UiKey.RIGHT_TO_LEFT to "右至左",
            UiKey.DIRECTION_AUTO_HINT to "跟隨檔案標示：目前%1\$s",
            UiKey.DIRECTION_MANUAL_HINT to "目前%1\$s翻頁",
            UiKey.DIRECTION_LTR_TEXT to "由左至右",
            UiKey.DIRECTION_RTL_TEXT to "由右至左",
            UiKey.PAGE_LAYOUT to "翻頁版面",
            UiKey.SINGLE_PAGE to "單頁",
            UiKey.DOUBLE_PAGE to "雙頁",
            UiKey.LAYOUT_HINT to "自動：直向顯示完整一頁；橫向或展開摺疊屏時左右各一頁，留白以白色填充。",
            UiKey.FONT_SIZE to "字體大小",
            UiKey.FONT_CURRENT to "目前 %1\$d sp；調整後會重新排版。",
            UiKey.IMAGE_BOOK_NOTE to "這是圖像書，字體大小不影響頁面。",
            UiKey.DISPLAY_EFFECTS to "顯示效果",
            UiKey.EYE_CARE to "護眼模式",
            UiKey.EYE_CARE_HINT to "為整個畫面加上暖色濾鏡，降低藍光。",
            UiKey.PAPER_GRAIN to "紙張顆粒",
            UiKey.PAPER_GRAIN_HINT to "在閱讀頁加上細微顆粒，模仿紙張質感。",
            UiKey.STRENGTH to "強度 %1\$d%%",
            UiKey.ZOOM_HINT to "雙指縮放頁面；放大後可單指拖動。",
            UiKey.READER_GESTURE_HINT to "點左右兩側翻頁，點中間顯示工具列。",
            UiKey.GLOBAL_SETTINGS to "全局設置",
            UiKey.SETTINGS_TITLE to "設置",
            UiKey.LANGUAGE to "語言",
            UiKey.TRADITIONAL_CHINESE to "繁體中文",
            UiKey.SIMPLIFIED_CHINESE to "简体中文",
            UiKey.ENGLISH to "English",
            UiKey.THEME to "外觀",
            UiKey.THEME_SYSTEM to "跟隨系統",
            UiKey.THEME_LIGHT to "淺色",
            UiKey.THEME_DARK to "深色",
            UiKey.READER_WHITE_NOTE to "閱讀頁固定為紙白色；護眼模式會再上一層暖色濾鏡。",
            UiKey.ABOUT to "關於",
            UiKey.ABOUT_SUMMARY to "為摺疊屏設計的 EPUB／TXT 閱讀器。提供單頁與雙頁排版、書籤、書庫搜尋、護眼模式與雙指縮放。",
            UiKey.DEVELOPER to "開發者",
            UiKey.VERSION to "版本 %1\$s",
            UiKey.GITHUB to "GitHub",
            UiKey.CLOSE to "關閉",
            UiKey.ERROR_FILE_READ to "無法讀取檔案。",
            UiKey.ERROR_NOT_EPUB to "這不是有效的 EPUB 檔案。",
            UiKey.ERROR_EMPTY_FILE to "檔案是空的。",
            UiKey.ERROR_TOO_LARGE to "檔案太大（超過 300 MB）。",
            UiKey.ERROR_UNSUPPORTED to "只支援 EPUB 或 TXT 檔案。",
            UiKey.ERROR_EPUB_CONTAINER to "這不是完整的 EPUB：缺少 container.xml。",
            UiKey.ERROR_EPUB_OPF to "找不到 EPUB 的內容目錄（OPF）。",
            UiKey.ERROR_EPUB_NO_SPINE to "這個 EPUB 沒有任何可閱讀的章節。",
            UiKey.ERROR_EPUB_NO_CONTENT to "找不到可顯示的內容，可能受 DRM 保護或檔案不完整。",
            UiKey.ERROR_TXT_EMPTY to "這個文字檔看起來是空的。",
            UiKey.ERROR_BOOK_MISSING to "找不到這本書，可能已被移除。",
            UiKey.ERROR_FILE_MISSING to "書籍檔案不存在，請重新匯入。",
            UiKey.ERROR_UNKNOWN to "發生未知錯誤。",
            UiKey.ERROR_STORAGE to "無法寫入書庫資料。",
        )

        val simplified = traditional.toMutableMap().apply {
            put(UiKey.APP_NAME, "苏打阅读")
            put(UiKey.BOOKS_COUNT, "共 %1\$d 本书")
            put(UiKey.BOOKS_COUNT_ONE, "共 1 本书")
            put(UiKey.SETTINGS_DESC, "设置")
            put(UiKey.SEARCH_DESC, "搜索书籍")
            put(UiKey.CLOSE_SEARCH, "关闭搜索")
            put(UiKey.SEARCH_HINT, "搜索书名或作者")
            put(UiKey.NO_RESULTS, "找不到符合的书")
            put(UiKey.NO_RESULTS_HINT, "试试其他关键词。")
            put(UiKey.IMPORT_BOOKS, "导入书籍")
            put(UiKey.IMPORTING, "正在导入书籍…")
            put(UiKey.CHOOSE_FILE, "选择文件")
            put(UiKey.SUPPORTED_FORMATS, "支持 .epub 与 .txt")
            put(UiKey.EMPTY_TITLE, "书库还是空的")
            put(UiKey.EMPTY_SUBTITLE, "导入 EPUB 漫画或 TXT 小说，\n直向一页、横向两页，慢慢看。")
            put(UiKey.MORE_OPTIONS, "《%1\$s》更多选项")
            put(UiKey.CONTINUE_READING, "继续阅读")
            put(UiKey.RESTART_READING, "从第一页重新开始")
            put(UiKey.REMOVE_FROM_LIBRARY, "从书库移除")
            put(UiKey.COVER_OF, "《%1\$s》封面")
            put(UiKey.READ_PERCENT, "已读 %1\$d%%")
            put(UiKey.REMOVE_TITLE, "移除这本书？")
            put(UiKey.REMOVE_MESSAGE, "《%1\$s》会从书库移除。你手机上的原始文件不受影响。")
            put(UiKey.REMOVE, "移除")
            put(UiKey.CANCEL, "取消")
            put(UiKey.RESTART_TITLE, "从第一页重新开始？")
            put(UiKey.RESTART_MESSAGE, "《%1\$s》目前的阅读进度会被清除。")
            put(UiKey.RESTART, "重新开始")
            put(UiKey.IMPORTED_BOOK, "已导入《%1\$s》")
            put(UiKey.REMOVED_BOOK, "已移除《%1\$s》")
            put(UiKey.LIBRARY_LOAD_FAILED, "读取书库失败。")
            put(UiKey.LOADING_LIBRARY, "正在整理书库…")
            put(UiKey.DELETE_FAILED, "移除失败。")
            put(UiKey.RESET_FAILED, "无法重置阅读进度。")
            put(UiKey.LOADING_BOOK, "正在载入书籍…")
            put(UiKey.CANNOT_OPEN, "无法打开这本书")
            put(UiKey.RETRY, "再试一次")
            put(UiKey.BACK_TO_LIBRARY, "返回书库")
            put(UiKey.RENDERING, "正在排版…")
            put(UiKey.NO_CONTENT, "没有可显示的内容")
            put(UiKey.PAGE_LOAD_FAILED, "第 %1\$d 页载入失败")
            put(UiKey.RETRY_PAGE, "重试")
            put(UiKey.READING_SETTINGS, "阅读设置")
            put(UiKey.PREVIOUS_PAGE, "上一页")
            put(UiKey.NEXT_PAGE, "下一页")
            put(UiKey.PAGE_OF, "第 %1\$d 页 / 共 %2\$d 页")
            put(UiKey.PAGES_OF, "第 %1\$d–%2\$d 页 / 共 %3\$d 页")
            put(UiKey.BOOKMARK, "书签")
            put(UiKey.ADD_BOOKMARK, "加入书签")
            put(UiKey.REMOVE_BOOKMARK, "移除书签")
            put(UiKey.BOOKMARK_AT, "书签：第 %1\$d 页")
            put(UiKey.READING_DIRECTION, "阅读方向")
            put(UiKey.AUTO, "自动")
            put(UiKey.LEFT_TO_RIGHT, "左至右")
            put(UiKey.RIGHT_TO_LEFT, "右至左")
            put(UiKey.DIRECTION_AUTO_HINT, "跟随文件标示：当前%1\$s")
            put(UiKey.DIRECTION_MANUAL_HINT, "当前%1\$s翻页")
            put(UiKey.DIRECTION_LTR_TEXT, "由左至右")
            put(UiKey.DIRECTION_RTL_TEXT, "由右至左")
            put(UiKey.PAGE_LAYOUT, "翻页版面")
            put(UiKey.SINGLE_PAGE, "单页")
            put(UiKey.DOUBLE_PAGE, "双页")
            put(UiKey.LAYOUT_HINT, "自动：直向显示完整一页；横向或展开折叠屏时左右各一页，留白以白色填充。")
            put(UiKey.FONT_SIZE, "字体大小")
            put(UiKey.FONT_CURRENT, "当前 %1\$d sp；调整后会重新排版。")
            put(UiKey.IMAGE_BOOK_NOTE, "这是图像书，字体大小不影响页面。")
            put(UiKey.DISPLAY_EFFECTS, "显示效果")
            put(UiKey.EYE_CARE, "护眼模式")
            put(UiKey.EYE_CARE_HINT, "为整个画面加上暖色滤镜，降低蓝光。")
            put(UiKey.PAPER_GRAIN, "纸张颗粒")
            put(UiKey.PAPER_GRAIN_HINT, "在阅读页加上细微颗粒，模仿纸张质感。")
            put(UiKey.STRENGTH, "强度 %1\$d%%")
            put(UiKey.ZOOM_HINT, "双指缩放页面；放大后可单指拖动。")
            put(UiKey.GLOBAL_SETTINGS, "全局设置")
            put(UiKey.SETTINGS_TITLE, "设置")
            put(UiKey.LANGUAGE, "语言")
            put(UiKey.THEME, "外观")
            put(UiKey.THEME_SYSTEM, "跟随系统")
            put(UiKey.THEME_LIGHT, "浅色")
            put(UiKey.THEME_DARK, "深色")
            put(UiKey.READER_WHITE_NOTE, "阅读页固定为纸白色；护眼模式会再叠加一层暖色滤镜。")
            put(UiKey.ABOUT, "关于")
            put(UiKey.ABOUT_SUMMARY, "为折叠屏设计的 EPUB／TXT 阅读器。提供单页与双页排版、书签、书库搜索、护眼模式与双指缩放。")
            put(UiKey.DEVELOPER, "开发者")
            put(UiKey.VERSION, "版本 %1\$s")
            put(UiKey.GITHUB, "GitHub")
            put(UiKey.CLOSE, "关闭")
            put(UiKey.ERROR_FILE_READ, "无法读取文件。")
            put(UiKey.ERROR_NOT_EPUB, "这不是有效的 EPUB 文件。")
            put(UiKey.ERROR_EMPTY_FILE, "文件是空的。")
            put(UiKey.ERROR_TOO_LARGE, "文件太大（超过 300 MB）。")
            put(UiKey.ERROR_UNSUPPORTED, "只支持 EPUB 或 TXT 文件。")
            put(UiKey.ERROR_EPUB_CONTAINER, "这不是完整的 EPUB：缺少 container.xml。")
            put(UiKey.ERROR_EPUB_OPF, "找不到 EPUB 的内容目录（OPF）。")
            put(UiKey.ERROR_EPUB_NO_SPINE, "这个 EPUB 没有任何可阅读的章节。")
            put(UiKey.ERROR_EPUB_NO_CONTENT, "找不到可显示的内容，可能受 DRM 保护或文件不完整。")
            put(UiKey.ERROR_TXT_EMPTY, "这个文本文件看起来是空的。")
            put(UiKey.ERROR_BOOK_MISSING, "找不到这本书，可能已被移除。")
            put(UiKey.ERROR_FILE_MISSING, "书籍文件不存在，请重新导入。")
            put(UiKey.ERROR_UNKNOWN, "发生未知错误。")
            put(UiKey.ERROR_STORAGE, "无法写入书库数据。")
            put(UiKey.READER_GESTURE_HINT, "点左右两侧翻页，点中间显示工具栏。")
        }

        val english = mapOf(
            UiKey.APP_NAME to "Soreada",
            UiKey.BOOKS_COUNT to "%1\$d books",
            UiKey.BOOKS_COUNT_ONE to "1 book",
            UiKey.SETTINGS_DESC to "Settings",
            UiKey.SEARCH_DESC to "Search books",
            UiKey.CLOSE_SEARCH to "Close search",
            UiKey.SEARCH_HINT to "Search title or author",
            UiKey.NO_RESULTS to "No matching books",
            UiKey.NO_RESULTS_HINT to "Try a different keyword.",
            UiKey.IMPORT_BOOKS to "Import",
            UiKey.IMPORTING to "Importing…",
            UiKey.CHOOSE_FILE to "Choose file",
            UiKey.SUPPORTED_FORMATS to "Supports .epub and .txt",
            UiKey.EMPTY_TITLE to "Your library is empty",
            UiKey.EMPTY_SUBTITLE to "Import EPUB comics or TXT novels.\nOne page upright, two pages wide.",
            UiKey.MORE_OPTIONS to "More options for \"%1\$s\"",
            UiKey.CONTINUE_READING to "Continue reading",
            UiKey.RESTART_READING to "Start from the beginning",
            UiKey.REMOVE_FROM_LIBRARY to "Remove from library",
            UiKey.COVER_OF to "Cover of \"%1\$s\"",
            UiKey.READ_PERCENT to "%1\$d%% read",
            UiKey.REMOVE_TITLE to "Remove this book?",
            UiKey.REMOVE_MESSAGE to "\"%1\$s\" will be removed from the library. The original file on your phone is untouched.",
            UiKey.REMOVE to "Remove",
            UiKey.CANCEL to "Cancel",
            UiKey.RESTART_TITLE to "Start over from page one?",
            UiKey.RESTART_MESSAGE to "Reading progress for \"%1\$s\" will be cleared.",
            UiKey.RESTART to "Start over",
            UiKey.IMPORTED_BOOK to "Imported \"%1\$s\"",
            UiKey.REMOVED_BOOK to "Removed \"%1\$s\"",
            UiKey.LIBRARY_LOAD_FAILED to "Could not read the library.",
            UiKey.LOADING_LIBRARY to "Organizing your library…",
            UiKey.DELETE_FAILED to "Could not remove the book.",
            UiKey.RESET_FAILED to "Could not reset reading progress.",
            UiKey.LOADING_BOOK to "Loading book…",
            UiKey.CANNOT_OPEN to "Can't open this book",
            UiKey.RETRY to "Try again",
            UiKey.BACK_TO_LIBRARY to "Back to library",
            UiKey.RENDERING to "Laying out text…",
            UiKey.NO_CONTENT to "Nothing to display",
            UiKey.PAGE_LOAD_FAILED to "Couldn't load page %1\$d",
            UiKey.RETRY_PAGE to "Retry",
            UiKey.READING_SETTINGS to "Reading settings",
            UiKey.PREVIOUS_PAGE to "Previous page",
            UiKey.NEXT_PAGE to "Next page",
            UiKey.PAGE_OF to "Page %1\$d of %2\$d",
            UiKey.PAGES_OF to "Pages %1\$d–%2\$d of %3\$d",
            UiKey.PAGE_SHORT to "%1\$d / %2\$d",
            UiKey.PAGE_NUMBER to "Page %1\$d",
            UiKey.BOOKMARK to "Bookmark",
            UiKey.ADD_BOOKMARK to "Add bookmark",
            UiKey.REMOVE_BOOKMARK to "Remove bookmark",
            UiKey.BOOKMARK_AT to "Bookmark: page %1\$d",
            UiKey.READING_DIRECTION to "Reading direction",
            UiKey.AUTO to "Auto",
            UiKey.LEFT_TO_RIGHT to "Left to right",
            UiKey.RIGHT_TO_LEFT to "Right to left",
            UiKey.DIRECTION_AUTO_HINT to "Following the file: %1\$s",
            UiKey.DIRECTION_MANUAL_HINT to "Reading %1\$s",
            UiKey.DIRECTION_LTR_TEXT to "left to right",
            UiKey.DIRECTION_RTL_TEXT to "right to left",
            UiKey.PAGE_LAYOUT to "Page layout",
            UiKey.SINGLE_PAGE to "Single",
            UiKey.DOUBLE_PAGE to "Two-page",
            UiKey.LAYOUT_HINT to "Auto: one full page upright; two pages landscape or unfolded, with white letterboxing.",
            UiKey.FONT_SIZE to "Font size",
            UiKey.FONT_CURRENT to "%1\$d sp; text will be reflowed.",
            UiKey.IMAGE_BOOK_NOTE to "This is an image book; font size doesn't apply.",
            UiKey.DISPLAY_EFFECTS to "Display effects",
            UiKey.EYE_CARE to "Eye care",
            UiKey.EYE_CARE_HINT to "Warm filter over the whole screen to reduce blue light.",
            UiKey.PAPER_GRAIN to "Paper grain",
            UiKey.PAPER_GRAIN_HINT to "Subtle grain on reading pages for a paper feel.",
            UiKey.STRENGTH to "Strength %1\$d%%",
            UiKey.ZOOM_HINT to "Pinch to zoom; drag with one finger when enlarged.",
            UiKey.READER_GESTURE_HINT to "Tap the sides to turn pages; tap the middle for controls.",
            UiKey.GLOBAL_SETTINGS to "Global settings",
            UiKey.SETTINGS_TITLE to "Settings",
            UiKey.LANGUAGE to "Language",
            UiKey.TRADITIONAL_CHINESE to "繁體中文",
            UiKey.SIMPLIFIED_CHINESE to "简体中文",
            UiKey.ENGLISH to "English",
            UiKey.THEME to "Appearance",
            UiKey.THEME_SYSTEM to "Follow system",
            UiKey.THEME_LIGHT to "Light",
            UiKey.THEME_DARK to "Dark",
            UiKey.READER_WHITE_NOTE to "Reading pages stay paper-white; eye care adds a warm filter.",
            UiKey.ABOUT to "About",
            UiKey.ABOUT_SUMMARY to "An EPUB/TXT reader designed for foldables, with single- and two-page layouts, bookmarks, library search, eye care, and pinch zoom.",
            UiKey.DEVELOPER to "Developer",
            UiKey.VERSION to "Version %1\$s",
            UiKey.GITHUB to "GitHub",
            UiKey.CLOSE to "Close",
            UiKey.ERROR_FILE_READ to "Could not read the file.",
            UiKey.ERROR_NOT_EPUB to "This is not a valid EPUB file.",
            UiKey.ERROR_EMPTY_FILE to "The file is empty.",
            UiKey.ERROR_TOO_LARGE to "The file is too large (over 300 MB).",
            UiKey.ERROR_UNSUPPORTED to "Only EPUB or TXT files are supported.",
            UiKey.ERROR_EPUB_CONTAINER to "Incomplete EPUB: container.xml is missing.",
            UiKey.ERROR_EPUB_OPF to "EPUB package document (OPF) not found.",
            UiKey.ERROR_EPUB_NO_SPINE to "This EPUB has no readable sections.",
            UiKey.ERROR_EPUB_NO_CONTENT to "No displayable content. The book may be DRM-protected or incomplete.",
            UiKey.ERROR_TXT_EMPTY to "This text file looks empty.",
            UiKey.ERROR_BOOK_MISSING to "This book is missing; it may have been removed.",
            UiKey.ERROR_FILE_MISSING to "The book file is missing; please import it again.",
            UiKey.ERROR_UNKNOWN to "Something went wrong.",
            UiKey.ERROR_STORAGE to "Could not save library data.",
        )
    }
}

val LocalUiStrings = staticCompositionLocalOf { UiStrings(AppLanguage.TRADITIONAL) }
