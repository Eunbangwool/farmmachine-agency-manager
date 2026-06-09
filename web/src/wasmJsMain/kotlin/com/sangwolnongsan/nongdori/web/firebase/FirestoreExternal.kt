@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.sangwolnongsan.nongdori.web.firebase

/**
 * Firestore JS interop (window.nongdori.* 헬퍼 호출).
 * 데이터 교환은 모두 JSON 문자열. Kotlin 측에서 kotlinx.serialization 으로 변환.
 * 도큐먼트는 항상 {id, ...data} 형태 (Android Firestore 구조와 호환).
 */

/** 컬렉션 실시간 구독. 콜백은 JSON 배열 문자열. */
@JsFun("(path, cb) => window.nongdori.observeCollection(path, cb)")
external fun jsObserveCollection(path: String, callback: (String) -> Unit)

/** 도큐먼트 set(upsert). data 는 JSON 문자열. */
@JsFun(
    """(path, jsonData, cb) => {
    window.nongdori.setDocByPath(path, jsonData)
        .then(() => cb('ok'))
        .catch(err => { console.warn('setDoc err', err); cb('err:' + (err.message || err)); });
}"""
)
external fun jsSetDoc(path: String, jsonData: String, callback: (String) -> Unit)

@JsFun(
    """(path, jsonData, cb) => {
    window.nongdori.updateDocByPath(path, jsonData)
        .then(() => cb('ok'))
        .catch(err => { console.warn('updateDoc err', err); cb('err:' + (err.message || err)); });
}"""
)
external fun jsUpdateDoc(path: String, jsonData: String, callback: (String) -> Unit)

@JsFun(
    """(path, cb) => {
    window.nongdori.deleteDocByPath(path)
        .then(() => cb('ok'))
        .catch(err => { console.warn('deleteDoc err', err); cb('err:' + (err.message || err)); });
}"""
)
external fun jsDeleteDoc(path: String, callback: (String) -> Unit)

/** 도큐먼트 한 번 읽기. 콜백은 JSON 문자열 (없으면 빈 문자열). */
@JsFun(
    """(path, cb) => {
    window.nongdori.getDocByPath(path)
        .then(json => cb(json))
        .catch(err => { console.warn('getDoc err', err); cb(''); });
}"""
)
external fun jsGetDoc(path: String, callback: (String) -> Unit)

/** 내가 멤버인 대리점 코드 목록. 콜백은 JSON 배열 문자열(["code1",...]). */
@JsFun("(cb) => window.nongdori.findMyDealerships(cb)")
external fun jsFindMyDealerships(callback: (String) -> Unit)

/** 텍스트 파일 download (CSV 내보내기 등). */
@JsFun("(filename, text, mimeType) => window.nongdori.downloadTextFile(filename, text, mimeType || '')")
external fun jsDownloadTextFile(filename: String, text: String, mimeType: String)

/** VWorld 지도 페이지를 새 탭으로 열어 해당 좌표에 마커 표시. */
@JsFun("(lat, lng, label) => window.nongdori.openVworldMap(lat, lng, label || '')")
external fun jsOpenVworldMap(lat: Double, lng: Double, label: String)
