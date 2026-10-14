# Verified offline LAN endpoint migration

Verified on 2026-10-15 (Asia/Shanghai).

The Samsung monitoring app was migrated from https://192.168.8.101:5443 to **https://192.168.8.103:5443** using the existing desktop QR pairing workflow. The user personally confirmed the camera scan, Verify and pair, and successful connection to the new endpoint. Samsung also opened the kiosk document-upload QR URL at http://192.168.8.103:3000.

No Android production code or production APK change was required for this IP-only migration. The existing trusted local CA, strict HTTPS/IP validation, bearer token and Keystore were preserved. The previous .101 pairing remains in encrypted rollback storage. No certificate, token, credential, QR image or machine security backup is included in this document or commit.

Verification passed for the new endpoint before pairing, encrypted rollback after pairing, 12 Samsung monitoring/security tests, and automatic recovery after a desktop restart. Dashboard Canon/Epson status, paper estimates/warnings, Current Printing and Print History matched desktop data. These tests ran on the original router Wi-Fi with mobile data off and no validated internet connection. File uploading, payment and physical printing were not tested.

**Known operational limitation:** 192.168.8.103 remains DHCP-assigned and is not permanently reserved. A later address change can invalidate the endpoint, certificate IP SAN and firewall binding. Reservation/static addressing is a separate approval task and was not performed.

For rollback, retain the saved pairing and Keystore. Restore a previous endpoint only after Windows actually owns that address and the matching desktop certificate/firewall configuration is available. Scan a fresh trusted desktop QR and explicitly Verify and pair; do not uninstall, clear app data or regenerate authentication credentials.

The wrap-up commit contains this migration document only. Earlier Android implementation changes and the existing untracked migration test are intentionally left uncommitted to keep pre-existing work out of the IP-only commit. This commit does not represent a standalone release of the earlier monitoring implementation.
