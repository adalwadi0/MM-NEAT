package gvgai.ontology.effects.binary;

import java.util.Collection;

import gvgai.core.content.InteractionContent;
import gvgai.core.game.Game;
import gvgai.core.logging.Logger;
import gvgai.core.logging.Message;
import gvgai.core.vgdl.VGDLFactory;
import gvgai.core.vgdl.VGDLSprite;
import gvgai.ontology.effects.Effect;
import gvgai.tools.Utils;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 04/11/13
 * Time: 15:56
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class TeleportToExit extends Effect
{

    public TeleportToExit(InteractionContent cnt)
    {
        this.parseParameters(cnt);
    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game)
    {
        int destinationId = VGDLFactory.GetInstance().requestFieldValueInt(sprite2, "itype");

        Collection<VGDLSprite> sprites = null;
        if(destinationId != -1){
            sprites = game.getSprites(destinationId);
        }
        else{
            Logger.getInstance().addMessage(new Message(Message.WARNING, "Ignoring TeleportToExit effect as " + sprite2.name + " isn't of type portal."));
            return;
        }

        if(sprites.size() > 0){
            VGDLSprite destination = (VGDLSprite) Utils.choice(sprites.toArray(), game.getRandomGenerator());
            sprite1.setRect(destination.rect);
            sprite1.lastmove = 0;

            if(destination.is_oriented)
            {
                sprite1.orientation = destination.orientation.copy();
            }
        }else{
            //If there is no exit... kill the sprite
            //boolean variable set to false to indicate the sprite was not transformed
            game.killSprite(sprite1, false);
        }
    }
}
