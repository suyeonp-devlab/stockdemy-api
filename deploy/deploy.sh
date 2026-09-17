#!/usr/bin/env bash
# 소스를 받아 EC2에서 직접 빌드하고 컨테이너를 교체한다
# 사용법: bash deploy.sh [app|api]  (생략하면 전체)
set -euo pipefail

# git pull로 이 스크립트가 바뀌어도 실행 중인 내용이 섞이지 않도록 전체를 함수로 읽은 뒤 실행
main() {

  local deploy_dir api_dir app_dir target services compose

  deploy_dir="$(cd "$(dirname "$0")" && pwd)"
  api_dir="$(cd "$deploy_dir/.." && pwd)"
  app_dir="$(cd "$deploy_dir/../../stockdemy-app" && pwd)"
  target="${1:-all}"

  compose="docker compose -f $deploy_dir/docker-compose.prod.yml --env-file $deploy_dir/.env"

  # 소스 받기
  case "$target" in
    app)
      git -C "$app_dir" pull
      services="app"
      ;;
    api)
      git -C "$api_dir" pull
      services="api"
      ;;
    all)
      git -C "$api_dir" pull
      git -C "$app_dir" pull
      services="api app"
      ;;
    *)
      echo "사용법: bash deploy.sh [app|api]"
      exit 1
      ;;
  esac

  # 빌드 (메모리가 작아 한 번에 하나씩)
  for service in $services; do
    $compose build "$service"
  done

  # 바뀐 컨테이너만 재생성
  $compose up -d

  # 이전 이미지 정리 (디스크 확보)
  docker image prune -f

  $compose ps
}

main "$@"
