package com.example.weathering.client;

import com.example.weathering.WeatheringConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class BloodDripParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   protected BloodDripParticle(ClientLevel world, double x, double y, double z, float r, float g, float b, SpriteSet sprites) {
      super(world, x, y, z, sprites.get(0, 1));
      this.sprites = sprites;
      this.gravity = 0.8F;
      this.friction = 0.98F;
      this.lifetime = (int)(12.0 / (Math.random() * 0.8 + 0.2));
      this.rCol = r;
      this.gCol = g;
      this.bCol = b;
      this.alpha = 0.95F;
      this.quadSize = 0.15F;
      this.setSpriteFromAge(sprites);
   }

   protected Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.yd = this.yd - 0.04 * this.gravity;
         this.move(this.xd, this.yd, this.zd);
         this.xd = this.xd * this.friction;
         this.yd = this.yd * this.friction;
         this.zd = this.zd * this.friction;
         this.setSpriteFromAge(this.sprites);
      }
   }

   public static class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Factory(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType type,
         ClientLevel world,
         double x,
         double y,
         double z,
         double velocityX,
         double velocityY,
         double velocityZ,
         RandomSource randomSource
      ) {
         int hex = WeatheringConfig.get().customBloodColor & 16777215;
         float r = (hex >> 16 & 0xFF) / 255.0F;
         float g = (hex >> 8 & 0xFF) / 255.0F;
         float b = (hex & 0xFF) / 255.0F;
         return new BloodDripParticle(world, x, y, z, r, g, b, this.sprites);
      }
   }
}
