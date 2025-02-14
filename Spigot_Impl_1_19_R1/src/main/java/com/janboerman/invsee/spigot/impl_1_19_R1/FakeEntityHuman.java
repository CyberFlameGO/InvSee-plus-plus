package com.janboerman.invsee.spigot.impl_1_19_R1;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.Level;

class FakeEntityHuman extends Player {

	FakeEntityHuman(Level world, BlockPos blockposition, float yaw, GameProfile gameprofile, ProfilePublicKey profilePublicKey) {
		super(world, blockposition, yaw, gameprofile, profilePublicKey);
	}

	@Override
	public boolean isCreative() {
		return false;
	}

	@Override
	public boolean isSpectator() {
		return false;
	}

}
