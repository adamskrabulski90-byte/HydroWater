package pl.hydrowater;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HydroLogic {
    private static int cooldown = 0;

    public static void tick(MinecraftClient mc) {
        HydroConfig cfg = HydroConfig.get();
        if (!cfg.enabled || mc.player == null || mc.world == null || mc.interactionManager == null
                || mc.currentScreen != null) return;
        if (cooldown > 0) { cooldown--; return; }

        ClientPlayerEntity p = mc.player;
        if (p.isSpectator() || p.isUsingItem()) return;

        int slot = findEmptyBucket(p);
        if (slot < 0) return; // potrzebne puste wiadro na hotbarze

        Vec3d eye = p.getEyePos();
        double reach = Math.min(cfg.range, p.getBlockInteractionRange());

        // 1. zbierz zrodla wody w zasiegu
        int r = (int) Math.ceil(reach) + 1;
        BlockPos base = BlockPos.ofFloored(eye);
        List<BlockPos> candidates = new ArrayList<>();
        double maxD = (reach + 0.9) * (reach + 0.9);
        for (int x = -r; x <= r; x++)
            for (int y = -r; y <= r; y++)
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = base.add(x, y, z);
                    FluidState fs = mc.world.getFluidState(pos);
                    if (fs.isOf(Fluids.WATER) && fs.isStill()
                            && eye.squaredDistanceTo(pos.toCenterPos()) <= maxD) {
                        candidates.add(pos.toImmutable());
                    }
                }
        candidates.sort(Comparator.comparingDouble(pos -> eye.squaredDistanceTo(pos.toCenterPos())));

        // 2. wybierz pierwszy blok, ktory mozna realnie zebrac
        for (BlockPos pos : candidates) {
            if (ownedByFriend(mc, pos)) continue;

            Vec3d c = pos.toCenterPos();
            double dx = c.x - eye.x, dy = c.y - eye.y, dz = c.z - eye.z;
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

            // limit rotacji: 360 = dowolny kierunek, 90 = tylko +-45 stopni od patrzenia
            float diff = Math.abs(MathHelper.wrapDegrees(yaw - p.getYaw()));
            if (cfg.rotation < 360 && diff > cfg.rotation / 2f) continue;

            // czy wiadro faktycznie trafi w zrodlo wody (nie w powietrze / blok)
            if (!canHitWater(mc, p, eye, yaw, pitch, reach)) continue;

            pickup(mc, p, slot, yaw, pitch);
            cooldown = cfg.delay;
            return;
        }
    }

    private static boolean canHitWater(MinecraftClient mc, ClientPlayerEntity p, Vec3d eye,
                                       float yaw, float pitch, double reach) {
        Vec3d look = Vec3d.fromPolar(pitch, yaw);
        Vec3d end = eye.add(look.multiply(reach));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, p));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        FluidState fs = mc.world.getFluidState(hit.getBlockPos());
        return fs.isOf(Fluids.WATER) && fs.isStill();
    }

    private static void pickup(MinecraftClient mc, ClientPlayerEntity p, int slot, float yaw, float pitch) {
        int prevSlot = p.getInventory().selectedSlot;
        float prevYaw = p.getYaw(), prevPitch = p.getPitch();

        p.getInventory().selectedSlot = slot;
        p.setYaw(yaw);       // obrot wysylany w pakiecie uzycia przedmiotu
        p.setPitch(pitch);
        ActionResult result = mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        p.setYaw(prevYaw);   // przywroc widok gracza
        p.setPitch(prevPitch);
        p.getInventory().selectedSlot = prevSlot;

        if (result.isAccepted()) p.swingHand(Hand.MAIN_HAND);
    }

    private static int findEmptyBucket(ClientPlayerEntity p) {
        int cur = p.getInventory().selectedSlot;
        if (p.getInventory().getStack(cur).isOf(Items.BUCKET)) return cur;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isOf(Items.BUCKET)) return i;
        }
        return -1;
    }

    /** Woda "nalezy" do najblizszego gracza (w 6 blokach). Jesli to znajomy - pomijamy. */
    private static boolean ownedByFriend(MinecraftClient mc, BlockPos pos) {
        Vec3d c = pos.toCenterPos();
        PlayerEntity best = null;
        double bestD = 36.0;
        for (PlayerEntity o : mc.world.getPlayers()) {
            double d = o.squaredDistanceTo(c);
            if (d < bestD) { bestD = d; best = o; }
        }
        return best != null && best != mc.player && HydroConfig.get().isFriend(best.getName().getString());
    }
}
