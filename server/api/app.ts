import * as Express from 'express';
import * as BodyParser from 'body-parser';
import * as mongoose from 'mongoose';
import {UserRoutes} from "./routes/user-routes";
import {AuthController} from "./security/auth-controller";
import {LogInRoutes} from "./routes/login-routes";

export class App {

    private readonly app: Express.Application;
    private readonly mongoUrl: string = 'mongodb://localhost/ProjectTracker';
    private readonly userRoutes: UserRoutes;
    private readonly logInRoutes: LogInRoutes;

    constructor() {
        this.app = Express();
        this.userRoutes = new UserRoutes();
        this.logInRoutes = new LogInRoutes();
        this.config();
        this.mongoSetup();

        // middlewares must be declared before the routes
        // this.setupMiddleWares();


        this.setupRoutes();
    }

    private config(): void{
        // support application/json type post data
        this.app.use(BodyParser.json());

        //support application/x-www-form-urlencoded post data
        this.app.use(BodyParser.urlencoded({ extended: false }));
    }

    private mongoSetup(): void{
        // @ts-ignore
        mongoose.Promise = global.Promise;
        mongoose.connect(this.mongoUrl);
    }

    private setupRoutes(): void {
        this.userRoutes.applyRoutes(this.app);
        this.logInRoutes.applyRoutes(this.app);
    }

    private setupMiddleWares() {
        this.app.use(AuthController.verifyToken);
    }

    public getApp(): Express.Application {
        return this.app;
    }
}

export default new App().getApp();