import * as Express from 'express';
import * as BodyParser from 'body-parser';
import * as mongoose from 'mongoose';
import {UserRoutes} from "./routes/user-routes";
import {AuthController} from "./security/auth-controller";
import {LogInRoutes} from "./routes/login-routes";
import * as jwt from 'express-jwt';
import {RoleRoutes} from "./routes/role-routes";
import {TrackerRoutes} from "./routes/tracker-routes";
import {HistoryRoutes} from "./routes/history-routes";

export class App {

    private readonly app: Express.Application;
    private readonly mongoUrl: string = 'mongodb://localhost/ProjectTracker';
    private readonly userRoutes: UserRoutes;
    private readonly logInRoutes: LogInRoutes;
    private readonly roleRoutes: RoleRoutes;
    private readonly trackerRoutes: TrackerRoutes;
    private readonly historyRoutes: HistoryRoutes;

    constructor() {
        this.app = Express();
        this.userRoutes = new UserRoutes();
        this.logInRoutes = new LogInRoutes();
        this.roleRoutes = new RoleRoutes();
        this.trackerRoutes = new TrackerRoutes();
        this.historyRoutes = new HistoryRoutes();
        this.config();
        this.mongoSetup();

        // middlewares must be declared before the routes
        this.setupMiddleWares();


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
        this.roleRoutes.applyRoutes(this.app);
        this.trackerRoutes.applyRoutes(this.app);
        this.historyRoutes.applyRoutes(this.app);
    }

    private setupMiddleWares() {
        // this.app.use(AuthController.verifyToken);
        this.app.use(
            jwt({
                secret: AuthController.secret,
                getToken: req => {
                    console.log(req.headers.authorization);
                    if (!req.headers.authorization) {
                        return null;
                    }

                    const words: string[] = req.headers.authorization.split(' ');
                    if (words.length != 2) {
                        // not the correct form
                        return null;
                    }

                    if (words[0] !== 'Bearer') {
                        // not the correct form of authentication
                        return null;
                    }

                    return words[1];
                }
            }).unless({
                path: ['/public/login', '/public/logout']
            })
        );
    }

    public getApp(): Express.Application {
        return this.app;
    }
}

export default new App().getApp();