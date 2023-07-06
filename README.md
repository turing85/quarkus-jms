# quarkus-jms Project

## Startup
- Start database and artemis: `cd local-deployment && docker compose up -d && cd ..`
- Start applications (in two different terminals):
  - 1st terminal: `./mvnw -f sender quarkus:dev`
  - 2nd terminal: `./mvnw -f receiver quarkus:dev`

## Cleanup
- Stop database and artemis: `cd local-deployment && docker compose down && cd`