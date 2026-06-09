# 농돌이 — 농기계 대리점 출장·수리 관리

농기계 **대리점**을 위한 현장 서비스 관리(FSM) 앱. 사무실 디스패처와 현장 엔지니어를 잇는다.

- **사무실 디스패처** → 웹(`:web`, Compose Multiplatform / WASM): 출장 생성·배정, 배차 보드, 고객 관리, 수리 이력
- **현장 엔지니어** → Android(`:app`): 배정된 출장 확인, 고객 정보·내비, 상태 진행, 수리 입력
- **공통 도메인·디자인** → `:shared` (Kotlin Multiplatform)

> 자매 앱: **농작이**(`farm-work-manager`, 농가용 농작업 관리), **농식이**(`farm-machine-manager`, 농기계 정비 관리).
> 본 앱은 두 앱의 컨셉을 합쳐 *대리점* 관점으로 재구성했으며, **별개의 Firebase 프로젝트**를 사용한다.

## 4대 기능
1. **출장관리** — 서비스콜(WorkOrder) 생성, 고객/기계 정보, 엔지니어 배정, 배차 보드, 라이브 위치/ETA
2. **수리진행상황** — 상태 워크플로 `접수 → 배정 → 출동 → 진행중 → 완료`(+ 취소)
3. **수리이력** — 고객별/기계별 완료 내역(작업·부품·금액)
4. **재고관리** — *PHASE 2 (예정)*. 현재는 모델 stub(`Part`) + `PartUsage.partId` 훅만 존재

## 모듈 구조
```
:shared   공유 도메인 모델(@Serializable) + 디자인 토큰/테마
:app      Android (현장 엔지니어)
:web      Compose/WASM 웹 (사무실 디스패처)
```
기술 스택: Kotlin 2.2.20, AGP 9.2.1, Compose Multiplatform 1.10.3, Firebase(Auth + Firestore).
패키지: `com.sangwolnongsan.nongdori`.

## Firestore 데이터 모델
```
dealerships/{dealerCode}
  members/{uid}            역할 OWNER/ADMIN/MEMBER (+ engineerProfile)
  joinRequests/{uid} · invitations/{email}
  customers/{customerId}
  machines/{machineId}     고객 보유 기계
  workOrders/{id}          출장 서비스콜 (RepairRecord 임베드, 고객/기계 스냅샷)
  liveLocations/{uid}      엔지니어 GPS
  parts/{partId}           재고 — PHASE 2 (현재 규칙 미정의 = 차단)
```
**수리 이력은 별도 컬렉션이 아니다** — `status == DONE` 인 WorkOrder 를 `customerId`/`machineId` 로 조회한다(FSM 표준).

## 최초 설정 (Firebase)
이 저장소는 코드만 포함한다. 실행하려면 **새 Firebase 프로젝트**가 필요하다.
1. [Firebase 콘솔](https://console.firebase.google.com)에서 새 프로젝트 생성 → Authentication(Google 공급자) + Firestore 활성화.
2. **Android 앱 등록**: 패키지명 `com.sangwolnongsan.nongdori` → `google-services.json` 다운로드 → `app/google-services.json` 에 배치 (`.gitignore` 처리됨).
3. **웹 앱 등록**: Firebase 웹 config 와 OAuth Web Client ID 발급.
   - Android: `local.properties` 에 `GOOGLE_WEB_CLIENT_ID=...` 또는 CI Secret.
   - Web: `web/.../index.html` 의 Firebase config (인증 단계에서 추가 예정).
4. Firestore 규칙 배포: `firebase deploy --only firestore:rules`.

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
- [ ] **7. 엔지니어 라이브 위치 / ETA**
- [ ] **8. 재고관리 (PHASE 2)**
