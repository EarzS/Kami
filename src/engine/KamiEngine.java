package engine;

import camera.TrailingCamera;
import character.Character;
import control.Controller;
import terrain.Terrain;
import world.World;
import world.WorldCreator;

class KamiEngine {
	private TrailingCamera camera;
	private Controller controller;
	private Painter painter;
	private World world;
	private Terrain terrain;
	private Character character;
	private boolean pause;
	
	KamiEngine() {
		int frame = 400, image = frame;
		Math3D.loadTrig(1000);
		controller = new Controller(frame, frame);
		painter = new Painter(frame, image, controller);
		camera = new TrailingCamera();
		terrain = new Terrain();
		createWorld();
	}
	
	private void createWorld() {
		WorldCreator wc = new WorldCreator(terrain.width / World.CHUNK_SIZE, terrain.length / World.CHUNK_SIZE, terrain.height / World.CHUNK_SIZE);
		world = wc.world;
		character = new Character(5, 5, 5);
		((TrailingCamera) camera).setFollow(character);
		world.addElement(character);
		world.initWorldElements();
	}
	
	void begin() {
		System.out.println("Begin");
		int frame = 0;
		long beginTime = 0, endTime;
		while (true) {
			while (pause) {
				checkPause();
				sleep(30);
			}
			painter.clear();
			camera.move(controller);
			camera.update(world.width, world.length, world.height);
			controller.setView(camera.angle, camera.orig(), camera.normal);
			world.update(terrain, controller);
			terrain.expand((int) character.getX(), (int) character.getY(), (int) character.getZ(), world);
			world.draw(painter, camera);
			if (controller.isKeyPressed(Controller.KEY_SLASH))
				painter.toggleWire();
			painter.paint();
			checkPause();
			sleep(10);
			endTime = System.nanoTime() + 1;
			if (endTime - beginTime > 1000000000L) {
				Painter.debugString[0] = "fps: " + frame + " ; paint surfaceCount: " + painter.surfaceCount + " ; paint drawCount: " + painter.drawCount;
				frame = 0;
				beginTime = endTime;
			}
			frame++;
		}
	}
	
	private void checkPause() {
		if (controller.isKeyPressed(Controller.KEY_P))
			pause = !pause;
	}
	
	static void sleep(int howLong) {
		try {
			Thread.sleep(howLong);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void printInstructions() {
		System.out.println("Kami - Java 3D voxel platformer . jar 3");
		System.out.println("W A S D to move");
		System.out.println("R F to move camera up / down");
		System.out.println("Z X to move camera farther / nearer");
		System.out.println("Press / to toggle wire mode");
		System.out.println("Space tap to jump");
		System.out.println("Space press to jet pack");
		System.out.println("Space press while running into a wall to climb the wall");
		System.out.println("Move mouse to move camera and direction");
		System.out.println("Press shift or press mouse button to throw grappling hook");
	}
	
	public static void main(String args[]) {
		KamiEngine.printInstructions();
		new KamiEngine().begin();
	}
}

// todo : graphics
// todo : shooting
// todo : survival
// todo : environment (e.g. clouds, zone types, world generation)
// todo : true boundless terrain/world