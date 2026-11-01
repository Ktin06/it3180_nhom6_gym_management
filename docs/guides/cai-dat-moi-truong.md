# Hướng dẫn cài đặt môi trường

Làm lần lượt từ trên xuống. Mỗi bước có lệnh kiểm tra; chỉ sang bước sau khi lệnh kiểm tra ra đúng kết quả. Trên Windows, mọi lệnh trong hướng dẫn chạy bằng **Git Bash** (cài cùng Git ở bước 1), trừ khi ghi rõ PowerShell.

Phiên bản cần có: JDK 21, Node.js 22, Docker Desktop, Git. Lý do chọn phiên bản: [ADR 0001](../decisions/0001-cong-nghe.md).

## 1. Cài Git

**Windows:** tải tại <https://git-scm.com/download/win>, cài với tùy chọn mặc định (có Git Bash). Hoặc trong PowerShell:

```powershell
winget install --id Git.Git -e
```

**macOS:**

```bash
xcode-select --install     # có sẵn git của Apple
# hoặc dùng Homebrew (https://brew.sh):
brew install git
```

Kiểm tra và khai báo tên, email (dùng đúng email tài khoản GitHub):

```bash
git --version
git config --global user.name "Họ Tên"
git config --global user.email "email-github-cua-ban@example.com"
```

## 2. Cài JDK 21

**Windows** (PowerShell):

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
```

Hoặc tải bộ cài `.msi` tại <https://adoptium.net/temurin/releases/?version=21> và **chọn "Set JAVA_HOME variable"** khi cài.

**macOS:**

```bash
brew install --cask temurin@21
```

Kiểm tra (mở cửa sổ terminal **mới** sau khi cài):

```bash
java -version      # phải ra "21.x"
echo $JAVA_HOME    # phải trỏ tới thư mục JDK 21
```

Nếu máy có nhiều JDK và `java -version` không ra 21, xem mục [Sai JAVA_HOME](#sai-java_home).

## 3. Cài Node.js 22

Nên dùng trình quản lý phiên bản để đổi phiên bản dễ dàng.

**Windows** (PowerShell) – dùng fnm:

```powershell
winget install Schniz.fnm
fnm env --use-on-cd --shell powershell | Out-String | Invoke-Expression   # thêm dòng này vào $PROFILE
fnm install 22
fnm default 22
```

Hoặc tải bộ cài Node 22 LTS tại <https://nodejs.org/en/download>.

**macOS** – dùng nvm:

```bash
brew install nvm      # làm theo hướng dẫn in ra để thêm nvm vào ~/.zshrc
nvm install 22
nvm alias default 22
```

Kiểm tra:

```bash
node -v    # phải ra v22.22.2 trở lên
npm -v
```

Thư mục `frontend/` có file `.nvmrc`; khi đứng trong thư mục đó, `nvm use` hoặc `fnm use` sẽ chọn đúng phiên bản.

## 4. Cài Docker Desktop

**Windows:**

1. Docker Desktop trên Windows cần **WSL 2**. Mở PowerShell **với quyền Administrator** và chạy:
   ```powershell
   wsl --install
   ```
   Khởi động lại máy khi được yêu cầu. Lệnh này bật cả "Virtual Machine Platform" và "Windows Subsystem for Linux".
2. Bật ảo hóa (Intel VT-x / AMD-V) trong BIOS nếu Task Manager → Performance → CPU ghi "Virtualization: Disabled".
3. Tải và cài Docker Desktop tại <https://www.docker.com/products/docker-desktop/> (chọn "Use WSL 2"), hoặc:
   ```powershell
   winget install --id Docker.DockerDesktop -e
   ```
4. Mở Docker Desktop, chờ biểu tượng cá voi báo "Engine running".

**macOS:** tải Docker Desktop đúng loại chip (Apple Silicon hoặc Intel) tại <https://www.docker.com/products/docker-desktop/>, hoặc `brew install --cask docker`, rồi mở ứng dụng Docker.

Kiểm tra:

```bash
docker --version
docker compose version
docker run --rm hello-world
```

## 5. Clone repo và bật hook Git

```bash
git clone https://github.com/Ktin06/it3180_nhom6_gym_management.git
cd it3180_nhom6_gym_management

# BẮT BUỘC, chạy NGAY SAU KHI CLONE: bật hook chặn đẩy thẳng lên main
git config core.hooksPath .githooks
```

Kiểm tra:

```bash
git config core.hooksPath     # phải ra .githooks
```

## 6. Tạo file .env

```bash
cp .env.example .env
```

Mở `.env` và sửa:

- `DB_PASSWORD`: mật khẩu bất kỳ cho PostgreSQL trên máy bạn.
- `JWT_SECRET`: chuỗi ngẫu nhiên dài ít nhất 32 ký tự (`openssl rand -base64 48` trong Git Bash hoặc macOS).
- `DB_PORT`: đổi thành `5433` nếu máy đã cài PostgreSQL khác (xem [Cổng 5432 bị chiếm](#cổng-5432-bị-chiếm)).

File `.env` không bao giờ được commit (đã có trong `.gitignore`) và không được dán vào prompt AI.

## 7. Bật PostgreSQL bằng Docker Compose

```bash
docker compose up -d
docker compose ps          # cột STATUS phải ra "healthy" (chờ khoảng 10 giây)
```

Các lệnh hữu ích:

```bash
docker compose logs -f postgres   # xem log
docker compose stop               # tắt, giữ dữ liệu
docker compose down -v            # xóa hẳn cả dữ liệu (cẩn thận)
```

## 8. Chạy backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Lần đầu chạy sẽ tải Maven và thư viện, mất vài phút. Khi log có dòng `Started GymManagementApplication`, kiểm tra:

```bash
curl http://localhost:8080/actuator/health     # phải ra {"status":"UP",...}
```

Chạy kiểm thử backend (cần Docker đang chạy):

```bash
./mvnw -B verify
```

Trên PowerShell thay `./mvnw` bằng `.\mvnw.cmd` và đặt tham số `-D...` trong dấu nháy kép.

## 9. Chạy frontend

Mở terminal thứ hai:

```bash
cd frontend
npm ci
npm run dev
```

Mở <http://localhost:5173>. Ở chế độ phát triển có ô **"Giả lập vai trò"** để thử menu theo từng vai trò (sẽ thay bằng đăng nhập thật ở F1).

Các lệnh khác: `npm run lint`, `npm test`, `npm run build`, `npm run format`.

## 10. Mở Swagger

Khi backend đang chạy, mở <http://localhost:8080/swagger-ui/index.html> để xem và thử các API.

## 11. Import Postman

1. Cài Postman tại <https://www.postman.com/downloads/>.
2. **Import** → chọn hai file trong thư mục `postman/`: `gym-management.postman_collection.json` và `local.postman_environment.json`.
3. Góc trên bên phải chọn environment **"Gym – local"**.
4. Mở thư mục **Hệ thống** → **Health check** → **Send**. Tab **Test Results** phải báo qua.

## Lỗi thường gặp

### Cổng 5432 bị chiếm

Triệu chứng: `docker compose up -d` báo `Bind for 0.0.0.0:5432 failed: port is already allocated` hoặc `ports are not available`.

Nguyên nhân: máy đã cài PostgreSQL (thường là dịch vụ `postgresql-x64-*` trên Windows) hoặc một container khác đang dùng cổng 5432.

Cách sửa (chọn một):

- Đổi cổng: trong `.env` đặt `DB_PORT=5433`, rồi chạy lại `docker compose up -d`. Backend đọc cùng biến nên không phải sửa gì thêm.
- Hoặc tắt PostgreSQL cài sẵn. Windows (PowerShell Administrator): `Stop-Service postgresql-x64-18` (đổi số phiên bản cho đúng). macOS: `brew services stop postgresql`.

Xem tiến trình đang chiếm cổng: Windows `netstat -ano | findstr :5432`; macOS `lsof -i :5432`.

### Docker chưa chạy

Triệu chứng: `Cannot connect to the Docker daemon`, `open //./pipe/docker_engine: The system cannot find the file specified`, hoặc kiểm thử backend báo `Could not find a valid Docker environment`.

Cách sửa: mở Docker Desktop và chờ "Engine running", rồi chạy lại `docker info`. Trên Windows, nếu Docker Desktop báo lỗi WSL hoặc "Virtual Machine Platform not enabled": chạy `wsl --install` trong PowerShell Administrator, khởi động lại máy, kiểm tra ảo hóa đã bật trong BIOS. Windows Home không có Hyper-V nên bắt buộc dùng WSL 2.

### Sai JAVA_HOME

Triệu chứng: `./mvnw` báo `JAVA_HOME is not defined correctly`, hoặc lỗi `release version 21 not supported`, hoặc `java -version` ra phiên bản khác 21.

Cách sửa:

- **Windows:** Start → "Edit the system environment variables" → Environment Variables → đặt `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-21...` (đúng thư mục trên máy), và trong `Path` đưa `%JAVA_HOME%\bin` lên trên các JDK khác. Mở lại terminal. Tạm thời trong Git Bash:
  ```bash
  export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot"   # sửa đúng tên thư mục
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- **macOS:** thêm vào `~/.zshrc`:
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 21)
  ```
  rồi `source ~/.zshrc`.

### Hook không chạy vì thiếu quyền thực thi

Triệu chứng: `git push` lên `main` không bị chặn, hoặc báo `hook was ignored because it's not set as executable`.

Cách sửa:

```bash
git config core.hooksPath          # phải ra .githooks; nếu trống thì chạy lại lệnh ở bước 5
chmod +x .githooks/pre-push        # macOS, Linux
ls -l .githooks/pre-push           # phải có chữ x: -rwxr-xr-x
```

Trên Windows, Git Bash chạy hook không cần `chmod`; nếu hook vẫn không chạy, kiểm tra file không bị đổi sang xuống dòng CRLF (repo đã cấu hình `.gitattributes` để giữ LF): `git ls-files --eol .githooks/pre-push` phải ra `i/lf w/lf`.

Thử hook mà không đẩy thật: `git push --dry-run origin HEAD:main` phải in "Không được đẩy thẳng lên main, hãy tạo nhánh và mở pull request". (Hook chỉ cho phép tạo `main` khi repo trên GitHub còn trống, tức lần đẩy commit đầu tiên của trưởng nhóm.)
