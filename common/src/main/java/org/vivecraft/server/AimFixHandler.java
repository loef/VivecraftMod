package org.vivecraft.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.vivecraft.server.config.ServerConfig;

public class AimFixHandler extends ChannelInboundHandlerAdapter {
    private final Connection netManager;
    private final ServerPlayer player;

    public AimFixHandler(Connection netManager, ServerPlayer player) {
        this.netManager = netManager;
        this.player = player;
    }

    /**
     * checks if the {@code msg}  uses the players aim, and changes it to the right position before handling
     *
     * @param ctx context when not handling the message
     * @param msg Packet to handle
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        boolean isCapturedPacket = msg instanceof ServerboundUseItemPacket ||
            msg instanceof ServerboundUseItemOnPacket ||
            msg instanceof ServerboundPlayerActionPacket;

        if (!ServerVRPlayers.isVRPlayer(this.player) || !isCapturedPacket) {
            // we don't need to handle this packet, just defer to the next handler in the pipeline
            ctx.fireChannelRead(msg);
            return;
        }

        this.player.theGame().server().submit(() -> {
            // Save all the current orientation data
            Vec3 pos = this.player.position();
            Vec3 prevPos = new Vec3(this.player.xo, this.player.yo, this.player.zo);
            float xRot = this.player.getXRot();
            float yRot = this.player.getYRot();
            float yHeadRot = this.player.yHeadRot;
            float prevXRot = this.player.xRotO;
            float prevYRot = this.player.yRotO;
            float prevYHeadRot = this.player.yHeadRotO;
            float eyeHeight = this.player.getEyeHeight();

            ServerVivePlayer vivePlayer = ServerVRPlayers.getVivePlayer(this.player);

            ((Packet) msg).handle(this.netManager.getPacketListener());
            if (true) {
                return;
            }
            Vec3 aimPos = null;
            // Check again in case of race condition
            if (vivePlayer != null && vivePlayer.isVR()) {
                aimPos = vivePlayer.getBodyPartPos(vivePlayer.activeBodyPart, true);
                Vec3 dir = vivePlayer.getBodyPartDir(vivePlayer.activeBodyPart);

                // Inject our custom orientation data
                this.player.setPosRaw(aimPos.x, aimPos.y, aimPos.z);
                this.player.xo = aimPos.x;
                this.player.yo = aimPos.y;
                this.player.zo = aimPos.z;
                this.player.setXRot((float) Math.toDegrees(Math.asin(-dir.y)));
                this.player.setYRot((float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
                this.player.xRotO = this.player.getXRot();
                this.player.yRotO = this.player.yHeadRotO = this.player.yHeadRot = this.player.getYRot();
                // non 0 to avoid divisions by 0
                this.player.eyeHeight = 0.0001F;

                // Set up offset to fix relative positions
                vivePlayer.offset = pos.subtract(aimPos);
                if (ServerConfig.DEBUG.get()) {
                    ServerNetworking.LOGGER.info("Vivecraft: AimFix: {} {} {}, {} {}", aimPos.x, aimPos.y, aimPos.z,
                        Math.toDegrees(Math.asin(-dir.y)), Math.toDegrees(Math.atan2(-dir.x, dir.z)));
                }
            }

            // Call the packet handler directly
            // This is several implementation details that we have to replicate
            try {
                if (this.netManager.isConnected()) {
                    try {
                        ((Packet) msg).handle(this.netManager.getPacketListener());
                    } catch (RunningOnDifferentThreadException ignored) {
                        // Apparently might get thrown and can be ignored
                    }
                }
            } finally {
                // Vanilla uses SimpleChannelInboundHandler, which automatically releases
                // by default, so we're expected to release the packet once we're done.
                ReferenceCountUtil.release(msg);
            }

            // if the packed changed the this.player position, use that
            if ((aimPos != null && !this.player.position().equals(aimPos)) ||
                (aimPos == null && !this.player.position().equals(pos)))
            {
                pos = this.player.position();
                if (ServerConfig.DEBUG.get()) {
                    ServerNetworking.LOGGER.info("Vivecraft: AimFix moved Player to: {} {} {}", pos.x, pos.y, pos.z);
                }
            }

            // Restore the original orientation data
            this.player.setPosRaw(pos.x, pos.y, pos.z);
            this.player.xo = prevPos.x;
            this.player.yo = prevPos.y;
            this.player.zo = prevPos.z;
            this.player.setXRot(xRot);
            this.player.setYRot(yRot);
            this.player.yHeadRot = yHeadRot;
            this.player.xRotO = prevXRot;
            this.player.yRotO = prevYRot;
            this.player.yHeadRotO = prevYHeadRot;
            this.player.eyeHeight = eyeHeight;

            // Reset offset
            if (vivePlayer != null) {
                vivePlayer.offset = Vec3.ZERO;
            }
        });
    }
}
