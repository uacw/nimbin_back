# Локальные переменные окружения для разработки
# Скопируйте эти команды в PowerShell перед запуском приложения

$env:JWT_SECRET="local-development-secret-key-change-in-production"
$env:JWT_ISSUER="http://localhost:8080/"
$env:JWT_AUDIENCE="http://localhost:8080/api"
$env:JWT_REALM="Access to 'api'"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/nimbin_dev"
$env:DB_USER="postgres"
$env:DB_PASSWORD="postgres"

# После установки переменных запустите приложение:
# .\gradlew.bat run
