import { User } from '../models/user';
import { Request, Response } from 'express';
import * as bcrypt from 'bcryptjs';
import * as jwt from 'jsonwebtoken';
import {AuthController} from "../security/auth-controller";

export class LogInController{

    constructor() {}

    public login(req: Request, res: Response): void {
        console.log(req.body);
        User.findOne({email: req.body.email})
            .exec((err: any, userDoc: Document) => {
                const user: any = userDoc;
                console.log(user);
                if (err) {
                    res.status(500).send(err);
                } else {
                    // check password
                    const match = bcrypt.compareSync(req.body.password, user.password);
                    if (match) {
                        // sign token
                        console.log(user.role);
                        const token = jwt.sign({ id: user._id, permissions: [user.role] }, AuthController.secret, {
                            expiresIn: 86400 // expires in 24 hours
                        });

                        res.status(200).json({auth: true, message: 'Success', token: token});
                    } else {
                        res.status(401).json({auth: false, message: 'Bad credentials'});
                    }
                }
            });
    }

    public logout(req: Request, res: Response) {
        res.status(200).json({auth: false, message: 'Successfully logged out!'});
    }
}
