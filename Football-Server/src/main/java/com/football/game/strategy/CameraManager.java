    package com.football.game.strategy;

    import com.football.util.math.geometry.Rotation2d;
    import com.football.util.math.geometry.Translation2d;

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

        public Translation2d getOrientedTranslation(Translation2d translation2d) {
            Rotation2d orientation = this.getPosition().getTranslation().toTranslation2d()
                    .minus(this.teamStrategy.getChosenPlayer().getPosition()).getAngle().plus(Rotation2d.kCCW_Pi_2);
            return translation2d.rotateBy(orientation);
        }
    }
