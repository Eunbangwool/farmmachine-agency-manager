# Claude Code 작업 규칙 (farmmachine-agency-manager / 농돌이)

## firestore.rules — ⚠️ 단독 배포 금지 (공유 프로젝트)

> 농돌이는 농작이(`farm-work-manager`)·농식이(`farm-machine-manager`)와 **같은 Firebase
> 프로젝트 `farm-machine-manager-prod`** 를 공유한다. Firestore 규칙은 **파일 전체 단위
> deploy** 다.

- 이 repo 의 `firestore.rules` 는 **참고용**(dealerships 부분만, farms 블록 없음).
  **절대 단독으로 `firebase deploy` 하지 말 것** — 배포하면 농작이/농식이 규칙이 통째로
  삭제된다.
- **배포 원본(canonical)** 은 `farm-work-manager/firestore.rules` (farms 보호블록 +
  dealerships 합본). 배포는 거기서 한다.
- dealerships 규칙을 바꾸면 **3곳 모두 sync**:
  `farm-work-manager/firestore.rules`, `farm-machine-manager/firestore.rules`,
  이 repo. (앞의 둘은 둘 다 배포 소스가 될 수 있어 동일 블록 필수.)

## Firebase / 빌드 메모

- Android applicationId(Firebase 등록 패키지): `com.sanwolnongsan.farmmachineagency`
  / 코드 네임스페이스: `com.sangwolnongsan.nongdori`.
- `app/google-services.json` 은 git 에 커밋하지 않음(public repo). CI 는 Secret
  `GOOGLE_SERVICES_JSON`(base64) 에서 복원. Google 로그인은 `GOOGLE_WEB_CLIENT_ID` +
  debug.keystore SHA-1 등록 필요.
