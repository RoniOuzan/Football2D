    package com.football.game.strategy;

    public class CameraManager {
        private transient final TeamStrategy teamStrategy;

        private CameraPose position;
        private CameraPositionType positionType;

        protected CameraManager(TeamStrategy teamStrategy) {
            this.teamStrategy = teamStrategy;
            this.positionType = CameraPositionType.BROADCAST;
            this.update();
        }

        public void update() {
            this.position = this.positionType.getCameraPose(this.teamStrategy);
        }

        public CameraPose getPosition() {
            return this.position;
        }

        public CameraPositionType getPositionType() {
            return this.positionType;
        }

        public void setPositionType(CameraPositionType positionType) {
            this.positionType = positionType;
        }
    }
