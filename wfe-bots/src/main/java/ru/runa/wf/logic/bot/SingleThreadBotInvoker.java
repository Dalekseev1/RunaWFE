/*
 * This file is part of the RUNA WFE project.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; version 2.1
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wf.logic.bot;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

import ru.runa.wfe.ConfigurationException;
import ru.runa.wfe.bot.Bot;
import ru.runa.wfe.bot.BotStation;
import ru.runa.wfe.bot.BotTask;
import ru.runa.wfe.bot.invoker.BotInvoker;
import ru.runa.wfe.commons.CoreErrorProperties;
import ru.runa.wfe.commons.SystemErrors;
import ru.runa.wfe.security.AuthenticationException;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.task.dto.WfTask;
import ru.runa.wfe.user.User;

public class SingleThreadBotInvoker implements BotInvoker {
    private final Log log = LogFactory.getLog(SingleThreadBotInvoker.class);
    private List<WorkflowBotExecutor> botExecutors;
    private long configurationVersion = -1;
    private BotStation botStation;

    @Override
    public synchronized void invokeBots(BotStation botStation, boolean resetFailedDelay) {
        this.botStation = Delegates.getBotService().getBotStation(botStation.getId());
        logBotsActivites();
        configure();
        for (WorkflowBotExecutor botExecutor : botExecutors) {
            try {
                if (resetFailedDelay) {
                    botExecutor.resetFailedDelay();
                }
                Set<WfTask> tasks = botExecutor.getNewTasks();
                for (WfTask task : tasks) {
                    WorkflowBotTaskExecutor botTaskExecutor = botExecutor.createBotTaskExecutor(task);
                    botTaskExecutor.run();
                }
            } catch (AuthenticationException e) {
                configurationVersion = -1;
                log.error("BotRunner execution failed. Will recreate botstation settings and bots.", e);
            } catch (Exception e) {
                log.error("BotRunner execution failed.", e);
            }
        }
    }

    private void configure() {
        String botStationErrorMessage = CoreErrorProperties.getMessage(CoreErrorProperties.BOT_STATION_CONFIGURATION_ERROR, botStation.getName());
        try {
            if (botStation.getVersion() != configurationVersion) {
                botExecutors = Lists.newArrayList();
                log.info("Will update bots configuration.");
                String username = BotStationResources.getSystemUsername();
                String password = BotStationResources.getSystemPassword();
                User botStationUser = Delegates.getAuthenticationService().authenticateByLoginPassword(username, password);
                List<Bot> bots = Delegates.getBotService().getBots(botStationUser, botStation.getId());
                for (Bot bot : bots) {
                    String botErrorMessage = CoreErrorProperties.getMessage(CoreErrorProperties.BOT_CONFIGURATION_ERROR, bot.getUsername());
                    try {
                        log.info("Configuring " + bot.getUsername());
                        User user = Delegates.getAuthenticationService().authenticateBot(botStationUser, bot);
                        List<BotTask> tasks = Delegates.getBotService().getBotTasks(user, bot.getId());
                        botExecutors.add(new WorkflowBotExecutor(user, bot, tasks));
                        SystemErrors.removeError(botErrorMessage);
                    } catch (Throwable th) {
                        log.error("Unable to configure " + bot, th);
                        SystemErrors.addError(new ConfigurationException(botErrorMessage, th));
                    }
                }
                configurationVersion = botStation.getVersion();
            } else {
                log.debug("bots configuration is up to date, version = " + botStation.getVersion());
            }
            SystemErrors.removeError(botStationErrorMessage);
        } catch (Throwable th) {
            log.error("Botstation configuration error", th);
            SystemErrors.addError(new ConfigurationException(botStationErrorMessage, th));
        }
    }

    private void logBotsActivites() {
        BotLogger botLogger = BotStationResources.createBotLogger();
        if (botLogger == null) {
            return;
        }
        botLogger.logActivity();
    }
}
