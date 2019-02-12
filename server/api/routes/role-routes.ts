import {Application} from "express";
import {RoleController} from "../controllers/role-controller";

export class RoleRoutes {

    private controller: RoleController;

    constructor() {
        this.controller = new RoleController();
    }

    public applyRoutes(app: Application): void {
        app.route('/resource/role')
            .get(this.controller.getAll)
            .post(this.controller.saveRole);
    }
}