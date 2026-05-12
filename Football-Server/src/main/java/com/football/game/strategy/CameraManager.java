    package com.football.game.strategy;

    import com.football.GameManager;
    import com.football.util.math.geometry.Rotation2d;
    import com.football.util.math.geometry.Translation2d;
    import lombok.Getter;
    import lombok.Setter;

    public class CameraManager {
        private static final double MAX_LINEAR_VELOCITY = 8;

        private transient final TeamStrategy teamStrategy;

        @Getter
        private CameraPose position;
        @Setter
        @Getter
        private CameraPositionType positionType;

        public CameraManager(TeamStrategy teamStrategy) {
            this.teamStrategy = teamStrategy;
            this.positionType = CameraPositionType.BROADCAST;

            this.position = new CameraPose();
            this.update();
        }

        public void update() {
            CameraPose targetPose = this.positionType.getCameraPose(this.teamStrategy);
            this.position = this.position.interpolate(targetPose, MAX_LINEAR_VELOCITY * GameManager.PERIOD);
        }

        public Translation2d getOrientedTranslation(Translation2d translation2d) {
            Rotation2d orientation = this.position.getTranslation().toTranslation2d()
                    .minus(this.teamStrategy.getChosenPlayer().getPosition()).getAngle().plus(Rotation2d.kCCW_Pi_2);
            return translation2d.rotateBy(orientation);
        }
    }
