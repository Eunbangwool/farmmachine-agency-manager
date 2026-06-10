# 농돌이 — 농기계 대리점 출장·수리 관리

농기계 **대리점**을 위한 현장 서비스 관리(FSM) 앱. 사무실 디스패처와 현장 엔지니어를 잇는다.

- **사무실 디스패처** → 웹(`:web`, Compose Multiplatform / WASM): 출장 생성·배정, 배차 보드, 고객 관리, 수리 이력
- **현장 엔지니어** → Android(`:app`): 배정된 출장 확인, 고객 정보·내비, 상태 진행, 수리 입력
- **공통 도메인·디자인** → `:shared` (Kotlin Multiplatform)

> 자매 앱: **농작이**(`farm-work-manager`, 농가용 농작업 관리), **농식이**(`farm-machine-manager`, 농기계 정비 관리).
> 본 앱은 두 앱의 컨셉을 합쳐 *대리점* 관점으로 재구성했으며, Firebase 프로젝트는 `farm-machine-manager-prod` 를 공유한다(앱별 패키지로 분리).

## 4대 기능
1. **출장관리** — 서비스콜(WorkOrder) 생성, 고객/기계 정보, 엔지니어 배정, 배차 보드, 라이브 위치/ETA
2. **수리진행상황** — 상태 워크플로 `접수 → 배정 → 출동 → 진행중 → 완료`(+ 취소)
3. **수리이력** — 고객별/기계별 완료 내역(작업·부품·금액)
4. **재고관리** — 부품 재고 등록/수정, 재고 +/- 조정, 재주문 기준 미달 강조 (웹 ‘재고’ 탭)

## 모듈 구조
```
:shared   공유 도메인 모델(@Serializable) + 디자인 토큰/테마
:app      Android (현장 엔지니어)
:web      Compose/WASM 웹 (사무실 디스패처)
```
기술 스택: Kotlin 2.2.20, AGP 9.2.1, Compose Multiplatform 1.10.3, Firebase(Auth + Firestore).
Android applicationId(Firebase 등록 패키지): `com.sanwolnongsan.farmmachineagency` · 코드 네임스페이스: `com.sangwolnongsan.nongdori`.

## Firestore 데이터 모델
```
dealerships/{dealerCode}
  members/{uid}            역할 OWNER/ADMIN/MEMBER (+ engineerProfile)
  joinRequests/{uid} · invitations/{email}
  customers/{customerId}
  machines/{machineId}     고객 보유 기계
  workOrders/{id}          출장 서비스콜 (RepairRecord 임베드, 고객/기계 스냅샷)
  liveLocations/{uid}      엔지니어 GPS
  parts/{partId}           재고 부품
```
**수리 이력은 별도 컬렉션이 아니다** — `status == DONE` 인 WorkOrder 를 `customerId`/`machineId` 로 조회한다(FSM 표준).

## 최초 설정 (Firebase)
Firebase 프로젝트 **`farm-machine-manager-prod`** 를 농작이·농식이와 공유한다(앱별 패키지로 분리).
1. **Android 앱 등록**: 패키지명 `com.sanwolnongsan.farmmachineagency` → `google-services.json`
   → `app/google-services.json` 에 배치 (`.gitignore` 처리됨, 저장소에 커밋 안 함).
2. **CI Secret** (public 저장소이므로 자격증명은 git 에 두지 않고 Secret 주입):
   - `GOOGLE_SERVICES_JSON` — `base64 -w0 app/google-services.json` 결과.
   - `GOOGLE_WEB_CLIENT_ID` — OAuth Web Client ID
     (`810632460995-r2t2uk3qb8kcp60uq2p25r6hht07ih1o.apps.googleusercontent.com`).
   - 저장소 Settings → Secrets and variables → Actions → New repository secret.
   - 로컬 빌드는 `local.properties` 에 `GOOGLE_WEB_CLIENT_ID=...`.
3. **Google 로그인 SHA-1**(권장): debug.keystore 의 SHA-1 을 Firebase Android 앱에 등록
   (Web Client ID idToken 방식이라 없어도 동작할 수 있으나 등록 권장).
4. **웹 앱**: `web/.../index.html` 의 Firebase web config (웹 주소/도메인은 현 설정 유지).
5. Firestore 규칙 배포: `firebase deploy --only firestore:rules`.
5. (선택) 기사 위치 VWorld 지도: [VWorld](https://www.vworld.kr) API 키 발급 →
   `web/.../index.html` 의 `VWORLD_KEY` 와 `map/engineer_map.html` 의 기본 키(TODO) 교체.

## 빌드
```bash
./gradlew :app:assembleDebug -PdebugBuild=true   # Android 디버그 APK
./gradlew :web:wasmJsBrowserDevelopmentRun        # 웹 로컬 실행
```
push 시 GitHub Actions(`.github/workflows/build.yml`)가 양 모듈 컴파일을 검증한다.

## 구축 로드맵 (점진)
- [x] **1. 골격** — Gradle 멀티플랫폼, 모듈, 브랜딩, 도메인 모델, 양 플랫폼 placeholder, firestore.rules, CI
- [x] **2. 인증/멤버십** — Google 로그인 + 대리점 코드 생성/가입 (Android UserManager/DealershipMembership, Web AuthManager/window.nongdori 브리지)
- [x] **3. 레포지토리 (웹)** — WorkOrder/Customer/Member (window.nongdori JSON 브리지 + kotlinx.serialization)
- [x] **4. 웹 출장 생성 → 배차 보드** — 출장 생성 폼(고객+기계+증상+우선순위+엔지니어 배정) + RepairStatus 칸반 보드(상태 전진)
- [x] **5. Android 엔지니어 — 출장 목록/상세/수리 입력** (배정 출장 목록, 고객 전화/길찾기, 상태 진행, 수리 입력→완료)
- [x] **6. 수리 이력 + CSV export** (웹 디스패처 ‘수리 이력’ 탭: 완료 출장 목록·금액 + CSV 내보내기)
- [x] **7. 엔지니어 라이브 위치 / ETA** (Android: 출동 시 위치 공유 Foreground Service; Web: ‘기사 위치’ 탭 — 마지막 위치·직선 ETA + VWorld 지도 페이지)
- [x] **8. 재고관리** (웹 ‘재고’ 탭: 부품 등록/수정, 재고 +/- 조정, 재주문 기준 미달 강조)
- [x] **+ 고객 관리** (웹 ‘고객’ 탭: 목록·검색, 상세=고객별 수리이력, 추가/수정/삭제, 출장 생성 시 기존 고객 재사용)
